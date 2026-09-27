package com.example.eldroid_nullpoint.util

import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Every Firestore *write* the borrower app performs, in one place.
 *
 * Reads stay in the activities as real-time snapshot listeners (they are UI
 * concerns); writes live here so the document shapes are defined once and match
 * `firestore.rules`. Borrow and return run inside a Firestore transaction that
 * re-reads the box first, so two phones racing for the same item cannot both win
 * (NFR-07), and the equipment update, transaction log entry and inbox notification
 * either all land or none do.
 */
object SmartDockRepository {

    /** Why a borrow or return was refused; mapped to a friendly message by the caller. */
    class LoanException(
        val borrowCheck: LoanPolicy.BorrowCheck? = null,
        val returnCheck: LoanPolicy.ReturnCheck? = null
    ) : Exception("Loan action refused")

    /** What the borrower is shown after a successful borrow (FR-05). */
    data class BorrowReceipt(
        val equipment: Equipment,
        val borrowedAt: Long,
        val dueAt: Long
    )

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ---------------------------------------------------------------
    // Borrow / return
    // ---------------------------------------------------------------

    fun borrow(equipmentId: String, uid: String, userName: String): Task<BorrowReceipt> {
        val db = firestore
        val equipmentRef = db.collection(Equipment.COLLECTION).document(equipmentId)
        return db.runTransaction { tx ->
            val snapshot = tx.get(equipmentRef)
            val current = if (snapshot.exists()) Equipment.from(snapshot) else null
            val check = LoanPolicy.canBorrow(current, uid)
            if (check != LoanPolicy.BorrowCheck.OK || current == null) {
                throw LoanException(borrowCheck = check)
            }

            val now = System.currentTimeMillis()
            tx.update(equipmentRef, LoanPolicy.borrowUpdate(uid, userName, now))
            tx.set(
                db.collection(Transaction.COLLECTION).document(),
                LoanPolicy.transactionDoc(current, uid, userName, Transaction.TYPE_BORROW, now)
            )
            val notification = LoanPolicy.eventNotification(
                current, uid, AppNotification.TYPE_BORROW, borrowedAt = now, now = now
            )
            tx.set(
                db.collection(AppNotification.COLLECTION).document(notification.id),
                notification.toMap()
            )
            BorrowReceipt(current, now, LoanPolicy.dueAtFor(now))
        }
    }

    fun returnItem(equipmentId: String, uid: String, userName: String): Task<Equipment> {
        val db = firestore
        val equipmentRef = db.collection(Equipment.COLLECTION).document(equipmentId)
        return db.runTransaction { tx ->
            val snapshot = tx.get(equipmentRef)
            val current = if (snapshot.exists()) Equipment.from(snapshot) else null
            val check = LoanPolicy.canReturn(current, uid)
            if (check != LoanPolicy.ReturnCheck.OK || current == null) {
                throw LoanException(returnCheck = check)
            }

            val now = System.currentTimeMillis()
            tx.update(equipmentRef, LoanPolicy.returnUpdate())
            tx.set(
                db.collection(Transaction.COLLECTION).document(),
                LoanPolicy.transactionDoc(current, uid, userName, Transaction.TYPE_RETURN, now)
            )
            // Keyed by the loan that is ending, so it stays unique per loan.
            val notification = LoanPolicy.eventNotification(
                current, uid, AppNotification.TYPE_RETURN, borrowedAt = current.borrowedAt, now = now
            )
            tx.set(
                db.collection(AppNotification.COLLECTION).document(notification.id),
                notification.toMap()
            )
            current
        }
    }

    /** Unwraps the transaction failure into the check that caused it, if any. */
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

    /** Creates the ten sample boxes. Only offered while the collection is empty. */
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
    // Notifications
    // ---------------------------------------------------------------

    /**
     * Writes a reminder under its deterministic id. Callers check for existence
     * first so the borrower is only alerted once per loan and type.
     */
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
