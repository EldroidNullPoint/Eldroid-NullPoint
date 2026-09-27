package com.example.eldroid_nullpoint.mvp.home

import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.DemoData
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.util.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomePresenter(
    private var view: HomeContract.View?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : HomeContract.Presenter {

    companion object {
        private const val RECENT_ACTIVITY_LIMIT = 4
        private const val EQUIPMENT_PREVIEW_LIMIT = 4
    }

    private var equipmentListener: ListenerRegistration? = null
    private var transactionListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private var equipmentLoaded = false
    private var transactionsLoaded = false
    private var isSeeding = false

    private var currentUid: String = ""
    private var borrowerName: String = ""

    override fun onStart(uid: String) {
        currentUid = uid

        // If listeners are already active (e.g. resumed from EquipmentDetail after a
        // borrow), just reload the greeting — the existing listeners keep the data live
        // and we must not flash the loading spinner unnecessarily.
        if (equipmentListener != null) {
            loadGreeting()
            return
        }

        equipmentLoaded = false
        transactionsLoaded = false
        view?.showLoading()
        loadGreeting()
        startListening()
    }

    override fun onStop() {
        stopListening()
    }

    override fun onRefresh() {
        stopListening()
        equipmentLoaded = false
        transactionsLoaded = false
        view?.showLoading()
        startListening()
    }

    override fun onSeedConfirmed() {
        if (isSeeding) return
        isSeeding = true
        view?.setSeeding(true)
        SmartDockRepository.seedEquipment()
            .addOnSuccessListener {
                isSeeding = false
                view?.setSeeding(false)
                view?.showToast("Sample equipment added")
            }
            .addOnFailureListener { error ->
                isSeeding = false
                view?.setSeeding(false)
                val msg = if (SmartDockRepository.isPermissionDenied(error)) {
                    "Permission denied. Contact your administrator."
                } else {
                    "Failed to add sample equipment"
                }
                view?.showToast(msg)
            }
    }

    // ---------------------------------------------------------------
    // Firestore listeners
    // ---------------------------------------------------------------

    private fun startListening() {
        listenToEquipment()
        listenToTransactions()
        listenToNotifications()
    }

    private fun listenToEquipment() {
        equipmentListener = firestore.collection(Equipment.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) {
                        equipmentLoaded = true
                        renderEquipment(DemoData.equipment(currentUid))
                        maybeShowContent()
                    } else {
                        view?.showError()
                    }
                    return@addSnapshotListener
                }
                val equipment = snapshot?.documents
                    ?.map { Equipment.from(it) }
                    ?.sortedBy { it.boxNumber }
                    .orEmpty()

                equipmentLoaded = true
                if (equipment.isEmpty() && DemoData.ENABLED) {
                    renderEquipment(DemoData.equipment(currentUid))
                } else {
                    renderEquipment(equipment)
                }
                maybeShowContent()
            }
    }

    private fun listenToTransactions() {
        transactionListener = firestore.collection(Transaction.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) {
                        transactionsLoaded = true
                        renderActivity(DemoData.transactions(currentUid, borrowerName))
                        maybeShowContent()
                    } else {
                        view?.showError()
                    }
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents
                    ?.map { Transaction.from(it) }
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

    private fun listenToNotifications() {
        notificationListener = firestore.collection(AppNotification.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, _ ->
                val unread = snapshot?.documents
                    ?.map { AppNotification.from(it) }
                    ?.count { !it.read } ?: 0
                view?.updateUnreadBadge(unread)
            }
    }

    private fun stopListening() {
        equipmentListener?.remove(); equipmentListener = null
        transactionListener?.remove(); transactionListener = null
        notificationListener?.remove(); notificationListener = null
    }

    // ---------------------------------------------------------------
    // Rendering helpers
    // ---------------------------------------------------------------

    private fun loadGreeting() {
        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.toObject(com.example.eldroid_nullpoint.model.User::class.java)
                    ?: com.example.eldroid_nullpoint.model.User(uid = currentUid)

                val firstName = profile.firstName.trim()
                val lastName  = profile.lastName.trim()
                borrowerName = listOf(firstName, lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")

                val prefix = greetingPrefix()
                view?.updateGreeting(
                    if (firstName.isBlank()) prefix else "$prefix, $firstName"
                )

                // Spec §3, §35 — show account/RFID status warnings
                view?.showAccountStatusBanner(profile.accountStatus, profile.rfidStatus)

                // Legacy RFID warning (kept for layout compatibility)
                view?.showRfidWarning(
                    profile.accountStatus == com.example.eldroid_nullpoint.model.User.ACCOUNT_ACTIVE &&
                    profile.rfidCardUid.isBlank()
                )
            }
    }

    private fun greetingPrefix(): String {
        return when (TimeFormat.greetingHour()) {
            TimeFormat.Greeting.MORNING -> "Good morning"
            TimeFormat.Greeting.AFTERNOON -> "Good afternoon"
            TimeFormat.Greeting.EVENING -> "Good evening"
        }
    }

    private fun renderEquipment(equipment: List<Equipment>) {
        view?.renderEquipmentList(equipment.take(EQUIPMENT_PREVIEW_LIMIT))

        val myItems = equipment
            .filter { it.isBorrowedBy(currentUid) }
            .sortedWith(compareBy { if (it.dueAt > 0L) it.dueAt else Long.MAX_VALUE })

        view?.renderCurrentItem(myItems.firstOrNull())

        if (myItems.size > 1) {
            view?.renderMyItems(myItems)
        } else {
            view?.renderMyItems(emptyList())
        }
    }

    private fun renderActivity(transactions: List<Transaction>) {
        view?.renderActivity(transactions.take(RECENT_ACTIVITY_LIMIT))
    }

    private fun maybeShowContent() {
        if (equipmentLoaded && transactionsLoaded) view?.showContent()
    }

    override fun detach() {
        stopListening()
        view = null
    }
}
