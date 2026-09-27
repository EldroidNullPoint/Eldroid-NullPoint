package com.example.eldroid_nullpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.EquipmentAdapter
import com.example.eldroid_nullpoint.adapter.TransactionAdapter
import com.example.eldroid_nullpoint.databinding.ActivityHomeBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.mvp.home.HomeContract
import com.example.eldroid_nullpoint.mvp.home.HomePresenter
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.NotificationPrefs
import com.example.eldroid_nullpoint.util.Notifier
import com.example.eldroid_nullpoint.util.TimeFormat
import com.example.eldroid_nullpoint.work.DueCheckWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity(), HomeContract.View {

    companion object {
        private const val UNREAD_BADGE_MAX = 9
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var presenter: HomeContract.Presenter

    private lateinit var equipmentAdapter: EquipmentAdapter
    private lateinit var myItemsAdapter: EquipmentAdapter
    private lateinit var activityAdapter: TransactionAdapter

    private var currentUid: String = ""
    
    private val debugListener: (String) -> Unit = { message ->
        runOnUiThread {
            binding.tvDebugBanner.text = "🔍 $message"
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) Notifier.ensureChannel(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }
        currentUid = currentUser.uid

        presenter = HomePresenter(this)

        setupRecyclerViews()
        setupHeaderActions()
        setupReminders()
        setupFirebaseConnectionMonitor()
    }

    override fun onStart() {
        super.onStart()
        com.example.eldroid_nullpoint.util.DebugBroadcaster.addListener(debugListener)
        if (currentUid.isNotBlank()) presenter.onStart(currentUid)
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.eldroid_nullpoint.util.DebugBroadcaster.removeListener(debugListener)
        presenter.detach()
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
        binding.ivRefresh.setOnClickListener { presenter.onRefresh() }
        binding.btnRetry.setOnClickListener { presenter.onRefresh() }
        binding.ivNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.btnSeeAllEquipment.setOnClickListener {
            startActivity(Intent(this, EquipmentListActivity::class.java))
        }
        binding.btnSeeAllActivity.setOnClickListener {
            startActivity(Intent(this, ActivityHistoryActivity::class.java))
        }
        binding.btnSeedEquipment.setOnClickListener {
            showSeedConfirmDialog()
        }
    }

    private fun setupReminders() {
        Notifier.ensureChannel(this)
        DueCheckWorker.schedule(this)
        DueCheckWorker.runNow(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            NotificationPrefs.isEnabled(this) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupFirebaseConnectionMonitor() {
        // Test Firestore connectivity
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("equipment")
            .limit(1)
            .get()
            .addOnSuccessListener {
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("Firestore: ✅ CONNECTED (read test passed)")
            }
            .addOnFailureListener { e ->
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("Firestore: ❌ FAILED - ${e.javaClass.simpleName}: ${e.message}")
            }
    }

    // ---------------------------------------------------------------
    // HomeContract.View
    // ---------------------------------------------------------------

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
    }

    override fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    override fun showError() {
        binding.progressBar.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
    }

    override fun updateGreeting(text: String) {
        binding.tvGreeting.text = text
    }

    override fun showRfidWarning(visible: Boolean) {
        binding.rfidWarning.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun renderEquipmentList(equipment: List<Equipment>) {
        equipmentAdapter.submit(equipment)
        binding.rvEquipment.visibility = if (equipment.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyEquipmentCard.visibility = if (equipment.isEmpty()) View.VISIBLE else View.GONE
        binding.btnSeeAllEquipment.visibility = if (equipment.isEmpty()) View.GONE else View.VISIBLE

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
    }

    override fun renderMyItems(myItems: List<Equipment>) {
        if (myItems.size > 1) {
            myItemsAdapter.submit(myItems)
            binding.titleMyItems.visibility = View.VISIBLE
            binding.rvMyItems.visibility = View.VISIBLE
        } else {
            binding.titleMyItems.visibility = View.GONE
            binding.rvMyItems.visibility = View.GONE
        }
    }

    override fun renderCurrentItem(item: Equipment?) {
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
        EquipmentImages.bindInto(binding.ivCurrentPhoto, item.name, item.category, fallbackPaddingDp = 15)

        val boxLabel = getString(R.string.box_label, item.boxNumber)
        binding.tvCurrentBox.text = if (item.category.isBlank()) boxLabel
        else "$boxLabel  ·  ${item.category}"

        binding.tvCurrentBorrowedAt.text = TimeFormat.dateTime(item.borrowedAt)
        binding.tvCurrentDueAt.text = TimeFormat.dateTime(item.dueAt)

        binding.tvCurrentStatusPill.text = TimeFormat.dueLabel(item.dueAt)
        val (pillBackground, pillTextColor) = when (TimeFormat.dueState(item.dueAt)) {
            TimeFormat.DueState.OVERDUE -> R.drawable.bg_pill_overdue to R.color.error_red
            TimeFormat.DueState.DUE_SOON -> R.drawable.bg_pill_due_soon to R.color.warning_amber
            else -> R.drawable.bg_pill_available to R.color.brand_dark_green
        }
        binding.tvCurrentStatusPill.setBackgroundResource(pillBackground)
        binding.tvCurrentStatusPill.setTextColor(ContextCompat.getColor(this, pillTextColor))
    }

    override fun renderActivity(transactions: List<Transaction>) {
        activityAdapter.submit(transactions)
        binding.rvActivity.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyActivity.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        binding.btnSeeAllActivity.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun updateUnreadBadge(unread: Int) {
        if (unread <= 0) {
            binding.tvUnreadBadge.visibility = View.GONE
            return
        }
        binding.tvUnreadBadge.visibility = View.VISIBLE
        binding.tvUnreadBadge.text = if (unread > UNREAD_BADGE_MAX) {
            getString(R.string.unread_badge_max)
        } else {
            unread.toString()
        }
    }

    override fun setSeeding(seeding: Boolean) {
        binding.btnSeedEquipment.isEnabled = !seeding
        binding.btnSeedEquipment.text = if (seeding) "" else getString(R.string.btn_seed_equipment)
        binding.progressSeed.visibility = if (seeding) View.VISIBLE else View.GONE
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showSeedConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.seed_confirm_title)
            .setMessage(R.string.seed_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> presenter.onSeedConfirmed() }
            .show()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
