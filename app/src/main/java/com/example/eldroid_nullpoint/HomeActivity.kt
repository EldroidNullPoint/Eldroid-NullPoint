package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.EquipmentAdapter
import com.example.eldroid_nullpoint.adapter.TransactionAdapter
import com.example.eldroid_nullpoint.databinding.ActivityHomeBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.DemoData
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.TimeFormat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Borrower dashboard.
 *
 * This is a monitoring surface, not a checkout screen: borrowing and returning
 * happen physically at the SmartDock tower via RFID, and the ESP32 writes the
 * result to Firestore. The app subscribes to those documents with snapshot
 * listeners so the screen tracks the tower in real time, and only ever reads.
 */
class HomeActivity : AppCompatActivity() {

    companion object {
        /**
         * The dashboard is a summary: each section previews only the top few rows
         * and defers the rest to its "See all" screen.
         */
        private const val RECENT_ACTIVITY_LIMIT = 4
        private const val EQUIPMENT_PREVIEW_LIMIT = 4
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var equipmentAdapter: EquipmentAdapter
    private lateinit var myItemsAdapter: EquipmentAdapter
    private lateinit var activityAdapter: TransactionAdapter

    private var equipmentListener: ListenerRegistration? = null
    private var transactionListener: ListenerRegistration? = null

    private var currentUid: String = ""

    /** Cached from the Firestore profile, used to label placeholder history rows. */
    private var borrowerName: String = ""

    /** Both listeners must deliver once before the loading spinner is dismissed. */
    private var equipmentLoaded = false
    private var transactionsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }
        currentUid = currentUser.uid

        setupRecyclerViews()
        setupHeaderActions()
        loadGreeting(currentUser.displayName)
    }

    override fun onStart() {
        super.onStart()
        // Re-subscribe whenever the screen comes forward (e.g. back from Change Password).
        if (currentUid.isNotBlank()) startListening()
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------

    private fun setupRecyclerViews() {
        val openDetail: (Equipment) -> Unit = { equipment ->
            startActivity(EquipmentDetailActivity.intent(this, equipment.id))
        }

        equipmentAdapter = EquipmentAdapter(currentUid, openDetail)
        myItemsAdapter = EquipmentAdapter(currentUid, openDetail)
        activityAdapter = TransactionAdapter { transaction ->
            if (transaction.equipmentId.isNotBlank()) {
                startActivity(EquipmentDetailActivity.intent(this, transaction.equipmentId))
            }
        }

        binding.rvEquipment.layoutManager = LinearLayoutManager(this)
        binding.rvEquipment.adapter = equipmentAdapter
        binding.rvEquipment.isNestedScrollingEnabled = false

        binding.rvMyItems.layoutManager = LinearLayoutManager(this)
        binding.rvMyItems.adapter = myItemsAdapter
        binding.rvMyItems.isNestedScrollingEnabled = false

        binding.rvActivity.layoutManager = LinearLayoutManager(this)
        binding.rvActivity.adapter = activityAdapter
        binding.rvActivity.isNestedScrollingEnabled = false
    }

    private fun setupHeaderActions() {
        binding.ivRefresh.setOnClickListener { restartListening() }
        binding.btnRetry.setOnClickListener { restartListening() }
        binding.ivChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
        binding.ivLogout.setOnClickListener { logout() }

        binding.btnSeeAllEquipment.setOnClickListener {
            startActivity(Intent(this, EquipmentListActivity::class.java))
        }
        binding.btnSeeAllActivity.setOnClickListener {
            startActivity(Intent(this, ActivityHistoryActivity::class.java))
        }
    }

    /**
     * "Good morning, Bryce". Prefers the Firestore profile name, falls back to the
     * Firebase Auth display name (set for Google accounts), then to no name at all.
     */
    private fun loadGreeting(displayName: String?) {
        val prefix = when (TimeFormat.greetingHour()) {
            TimeFormat.Greeting.MORNING -> getString(R.string.greeting_morning)
            TimeFormat.Greeting.AFTERNOON -> getString(R.string.greeting_afternoon)
            TimeFormat.Greeting.EVENING -> getString(R.string.greeting_evening)
        }

        val fallbackName = displayName?.trim()?.split(" ")?.firstOrNull().orEmpty()
        binding.tvGreeting.text = greetingText(prefix, fallbackName)

        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { snapshot ->
                val firstName = snapshot.getString("firstName").orEmpty().trim()
                val lastName = snapshot.getString("lastName").orEmpty().trim()
                borrowerName = listOf(firstName, lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")

                binding.tvGreeting.text =
                    greetingText(prefix, firstName.ifBlank { fallbackName })

                // No registered card means no session can be opened at the tower
                // (FR-01), so warn rather than let them find out at the reader.
                val rfidCardUid = snapshot.getString("rfidCardUid").orEmpty().trim()
                binding.rfidWarning.visibility =
                    if (rfidCardUid.isBlank()) View.VISIBLE else View.GONE
            }
        // A failed profile read is not worth an error state - the greeting simply
        // stays on the fallback above and the dashboard data is unaffected.
    }

    private fun greetingText(prefix: String, name: String): String =
        if (name.isBlank()) prefix else "$prefix, $name"

    // ---------------------------------------------------------------
    // Firestore real-time listeners
    // ---------------------------------------------------------------

    private fun restartListening() {
        stopListening()
        startListening()
    }

    private fun startListening() {
        if (equipmentListener != null || transactionListener != null) return

        equipmentLoaded = false
        transactionsLoaded = false
        showLoading()

        // ---- Equipment: every SmartDock box, ordered by box number ----
        equipmentListener = firestore.collection(Equipment.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // With placeholder content switched on, an unreadable collection
                    // still shows a populated dashboard rather than a dead end.
                    if (DemoData.ENABLED) {
                        equipmentLoaded = true
                        renderEquipment(DemoData.equipment(currentUid))
                        maybeShowContent()
                    } else {
                        showError()
                    }
                    return@addSnapshotListener
                }
                val equipment = snapshot?.documents
                    ?.map { Equipment.from(it) }
                    ?.sortedBy { it.boxNumber }
                    .orEmpty()

                equipmentLoaded = true
                // Real documents always win; the sample set only fills an empty tower.
                if (equipment.isEmpty() && DemoData.ENABLED) {
                    renderEquipment(DemoData.equipment(currentUid))
                } else {
                    renderEquipment(equipment)
                }
                maybeShowContent()
            }

        // ---- Recent activity: this borrower's own transactions ----
        // Filtering by uid alone keeps this to an equality query, which needs no
        // composite index; the ordering is done client-side. Per-borrower
        // transaction volume is small in the MVP. If it grows, add the
        // (uid ASC, timestamp DESC) index and move the sort/limit into the query.
        transactionListener = firestore.collection(Transaction.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) {
                        transactionsLoaded = true
                        renderActivity(DemoData.transactions(currentUid, borrowerName))
                        maybeShowContent()
                    } else {
                        showError()
                    }
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents
                    ?.map { Transaction.from(it) }
                    // Unauthorised-access alerts are logged with an empty uid and are
                    // an administrator concern; they never reach this list anyway
                    // because of the uid filter, but guard explicitly for clarity.
                    ?.filter { it.uid.isNotBlank() }
                    ?.sortedByDescending { it.timestamp }
                    .orEmpty()

                transactionsLoaded = true
                if (transactions.isEmpty() && DemoData.ENABLED) {
                    renderActivity(DemoData.transactions(currentUid, borrowerName))
                } else {
                    renderActivity(transactions)
                }
                maybeShowContent()
            }
    }

    private fun stopListening() {
        equipmentListener?.remove()
        equipmentListener = null
        transactionListener?.remove()
        transactionListener = null
    }

    // ---------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------

    private fun renderEquipment(equipment: List<Equipment>) {
        // Availability preview - the rest lives behind "See all".
        equipmentAdapter.submit(equipment.take(EQUIPMENT_PREVIEW_LIMIT))
        binding.rvEquipment.visibility = if (equipment.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyEquipment.visibility = if (equipment.isEmpty()) View.VISIBLE else View.GONE

        binding.btnSeeAllEquipment.visibility =
            if (equipment.size > EQUIPMENT_PREVIEW_LIMIT) View.VISIBLE else View.GONE

        if (equipment.isEmpty()) {
            binding.tvAvailableCount.visibility = View.GONE
        } else {
            binding.tvAvailableCount.visibility = View.VISIBLE
            binding.tvAvailableCount.text = getString(
                R.string.available_count,
                equipment.count { !it.isBorrowed },
                equipment.size
            )
        }

        // Items this borrower is currently holding, most urgent first.
        val myItems = equipment
            .filter { it.isBorrowedBy(currentUid) }
            .sortedWith(compareBy { if (it.dueAt > 0L) it.dueAt else Long.MAX_VALUE })

        renderCurrentStatus(myItems.firstOrNull())

        // The hero card above already covers a single item, so the list is only
        // worth showing once the borrower is holding more than one.
        if (myItems.size > 1) {
            myItemsAdapter.submit(myItems)
            binding.titleMyItems.visibility = View.VISIBLE
            binding.rvMyItems.visibility = View.VISIBLE
        } else {
            binding.titleMyItems.visibility = View.GONE
            binding.rvMyItems.visibility = View.GONE
        }
    }

    private fun renderCurrentStatus(item: Equipment?) {
        if (item == null) {
            binding.cardCurrentItem.visibility = View.GONE
            binding.emptyCurrentItem.visibility = View.VISIBLE
            return
        }

        binding.emptyCurrentItem.visibility = View.GONE
        binding.cardCurrentItem.visibility = View.VISIBLE
        binding.cardCurrentItem.setOnClickListener {
            startActivity(EquipmentDetailActivity.intent(this, item.id))
        }

        binding.tvCurrentName.text = item.name.ifBlank { "Unnamed equipment" }

        EquipmentImages.bindInto(
            binding.ivCurrentPhoto,
            item.name,
            item.category,
            fallbackPaddingDp = 15
        )

        val boxLabel = getString(R.string.box_label, item.boxNumber)
        binding.tvCurrentBox.text = if (item.category.isBlank()) {
            boxLabel
        } else {
            "$boxLabel  ·  ${item.category}"
        }

        binding.tvCurrentBorrowedAt.text = TimeFormat.dateTime(item.borrowedAt)
        binding.tvCurrentDueAt.text = TimeFormat.dateTime(item.dueAt)

        // "Due in 3h 20m" / "Due soon" / "Overdue by 2d 4h", colour-coded.
        binding.tvCurrentStatusPill.text = TimeFormat.dueLabel(item.dueAt)
        val (pillBackground, pillTextColor) = when (TimeFormat.dueState(item.dueAt)) {
            TimeFormat.DueState.OVERDUE ->
                R.drawable.bg_pill_overdue to R.color.error_red
            TimeFormat.DueState.DUE_SOON ->
                R.drawable.bg_pill_due_soon to R.color.warning_amber
            else ->
                R.drawable.bg_pill_available to R.color.brand_dark_green
        }
        binding.tvCurrentStatusPill.setBackgroundResource(pillBackground)
        binding.tvCurrentStatusPill.setTextColor(ContextCompat.getColor(this, pillTextColor))
    }

    private fun renderActivity(transactions: List<Transaction>) {
        activityAdapter.submit(transactions.take(RECENT_ACTIVITY_LIMIT))
        binding.rvActivity.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyActivity.visibility =
            if (transactions.isEmpty()) View.VISIBLE else View.GONE

        binding.btnSeeAllActivity.visibility =
            if (transactions.size > RECENT_ACTIVITY_LIMIT) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------
    // Loading / error / content states
    // ---------------------------------------------------------------

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
    }

    private fun maybeShowContent() {
        if (!equipmentLoaded || !transactionsLoaded) return
        binding.progressBar.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    private fun showError() {
        stopListening()
        binding.progressBar.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------
    // Session
    // ---------------------------------------------------------------

    private fun logout() {
        stopListening()
        auth.signOut()

        // Also sign out of Google so the account picker shows again next time.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut()

        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
