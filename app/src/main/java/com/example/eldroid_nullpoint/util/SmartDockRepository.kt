package com.example.eldroid_nullpoint.util

import android.util.Log
import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Every Firestore write the borrower app performs, in one place.
 *
 * Borrow and return run inside a Firestore transaction that re-reads the box
 * first, so two phones racing for the same item cannot both win (NFR-07).
 */
object SmartDockRepository {

    private const val TAG = "SmartDockRepo"

    class LoanException(
        val borrowCheck: LoanPolicy.BorrowCheck? = null,
        val returnCheck: LoanPolicy.ReturnCheck? = null
    ) : Exception("Loan action refused")

    data class BorrowReceipt(
        val equipment: Equipment,
        val borrowedAt: Long,
        val dueAt: Long
    )

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ---------------------------------------------------------------
    // Borrow
    // ---------------------------------------------------------------

    fun borrow(equipmentId: String, uid: String, userName: String): Task<BorrowReceipt> {
        val db = firestore
        val equipmentRef = db.collection(Equipment.COLLECTION).document(equipmentId)

        return db.runTransaction { tx ->
            val snapshot = tx.get(equipmentRef)
            val current = if (snapshot.exists()) Equipment.from(snapshot) else null
            val check = LoanPolicy.canBorrow(current, uid)

            if (check != LoanPolicy.BorrowCheck.OK || current == null) {
                Log.w(TAG, "borrow blocked: $check")
                throw LoanException(borrowCheck = check)
            }

            val now = System.currentTimeMillis()
            val dueAt = LoanPolicy.dueAtFor(now)

            // 1. Update equipment status
            tx.update(equipmentRef, LoanPolicy.borrowUpdate(uid, userName, now))

            // 2. Append transaction log
            tx.set(
                db.collection(Transaction.COLLECTION).document(),
                LoanPolicy.transactionDoc(current, uid, userName, Transaction.TYPE_BORROW, now)
            )

            // 3. Write borrow confirmation notification
            val notification = LoanPolicy.eventNotification(
                current, uid, AppNotification.TYPE_BORROW, borrowedAt = now, now = now
            )
            tx.set(
                db.collection(AppNotification.COLLECTION).document(notification.id),
                notification.toMap()
            )

            BorrowReceipt(current, now, dueAt)
        }.addOnSuccessListener {
            Log.d(TAG, "borrow success: equipmentId=$equipmentId")
        }.addOnFailureListener { e ->
            Log.e(TAG, "borrow FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    // ---------------------------------------------------------------
    // Return
    // ---------------------------------------------------------------

    fun returnItem(equipmentId: String, uid: String, userName: String): Task<Equipment> {
        val db = firestore
        val equipmentRef = db.collection(Equipment.COLLECTION).document(equipmentId)

        return db.runTransaction { tx ->
            val snapshot = tx.get(equipmentRef)
            val current = if (snapshot.exists()) Equipment.from(snapshot) else null
            val check = LoanPolicy.canReturn(current, uid)

            if (check != LoanPolicy.ReturnCheck.OK || current == null) {
                Log.w(TAG, "return blocked: $check")
                throw LoanException(returnCheck = check)
            }

            val now = System.currentTimeMillis()

            // 1. Clear equipment back to available
            tx.update(equipmentRef, LoanPolicy.returnUpdate())

            // 2. Append transaction log
            tx.set(
                db.collection(Transaction.COLLECTION).document(),
                LoanPolicy.transactionDoc(current, uid, userName, Transaction.TYPE_RETURN, now)
            )

            // 3. Write return confirmation notification
            val notification = LoanPolicy.eventNotification(
                current, uid, AppNotification.TYPE_RETURN,
                borrowedAt = current.borrowedAt, now = now
            )
            tx.set(
                db.collection(AppNotification.COLLECTION).document(notification.id),
                notification.toMap()
            )

            current
        }.addOnSuccessListener {
            Log.d(TAG, "return success: equipmentId=$equipmentId")
        }.addOnFailureListener { e ->
            Log.e(TAG, "return FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    fun loanExceptionOf(error: Throwable?): LoanException? {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is LoanException) return cause
            cause = cause.cause
        }
        return null
    }

    fun isPermissionDenied(error: Throwable?): Boolean =
        (error as? FirebaseFirestoreException)?.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED

    // ---------------------------------------------------------------
    // Seeding
    // ---------------------------------------------------------------

    fun seedEquipment(): Task<Void> {
        val db = firestore
        val batch = db.batch()
        SeedEquipment.BOXES.forEach { box ->
            batch.set(
                db.collection(Equipment.COLLECTION).document(box.id),
                SeedEquipment.toMap(box)
            )
        }
        return batch.commit()
    }

    // ---------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------

    fun updateProfile(
        uid: String,
        firstName: String,
        lastName: String,
        rfidCardUid: String,
        notificationsEnabled: Boolean
    ): Task<Void> = firestore.collection("users").document(uid).update(
        mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "rfidCardUid" to rfidCardUid,
            "notificationsEnabled" to notificationsEnabled
        )
    )

    // ---------------------------------------------------------------
    // Extension Requests  (spec §15)
    // ---------------------------------------------------------------

    /**
     * Submits an extension request for an active loan.
     * Android must never directly edit `dueAt` — it only creates this document
     * and the admin approves/rejects via Web Admin.
     */
    fun submitExtensionRequest(
        request: com.example.eldroid_nullpoint.model.ExtensionRequest
    ): Task<Void> = firestore
        .collection(com.example.eldroid_nullpoint.model.ExtensionRequest.COLLECTION)
        .document(request.id)
        .set(request.toMap())

    /** Live stream of this borrower's extension requests. */
    fun extensionRequestsForUser(
        uid: String
    ) = firestore
        .collection(com.example.eldroid_nullpoint.model.ExtensionRequest.COLLECTION)
        .whereEqualTo("uid", uid)

    // ---------------------------------------------------------------
    // Notifications
    // ---------------------------------------------------------------

    fun saveNotification(notification: AppNotification): Task<Void> =
        firestore.collection(AppNotification.COLLECTION)
            .document(notification.id)
            .set(notification.toMap())

    fun markNotificationRead(id: String): Task<Void> =
        firestore.collection(AppNotification.COLLECTION).document(id).update("read", true)

    fun markNotificationsRead(ids: List<String>): Task<Void> {
        val db = firestore
        val batch = db.batch()
        ids.forEach { id ->
            batch.update(db.collection(AppNotification.COLLECTION).document(id), "read", true)
        }
        return batch.commit()
    }
}
