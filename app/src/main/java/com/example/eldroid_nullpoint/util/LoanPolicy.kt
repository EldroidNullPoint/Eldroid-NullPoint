package com.example.eldroid_nullpoint.util

import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction

/**
 * The borrowing rules, expressed as pure functions so they can be unit tested and
 * so the Firestore write maps here stay in lockstep with `firestore.rules`
 * (`isBorrow()` / `isReturn()` there validate exactly these shapes).
 */
object LoanPolicy {

    /** Every loan is due 24 hours after it starts. */
    const val LOAN_DURATION_MS = 24 * 60 * 60 * 1000L

    fun dueAtFor(borrowedAt: Long): Long = borrowedAt + LOAN_DURATION_MS

    enum class BorrowCheck { OK, NOT_SIGNED_IN, NOT_FOUND, ALREADY_BORROWED, UNAVAILABLE }
    enum class ReturnCheck { OK, NOT_SIGNED_IN, NOT_FOUND, NOT_BORROWED, NOT_YOURS }

    fun canBorrow(equipment: Equipment?, uid: String): BorrowCheck = when {
        uid.isBlank() -> BorrowCheck.NOT_SIGNED_IN
        equipment == null -> BorrowCheck.NOT_FOUND
        equipment.isUnavailable -> BorrowCheck.UNAVAILABLE
        equipment.isBorrowed -> BorrowCheck.ALREADY_BORROWED
        else -> BorrowCheck.OK
    }

    fun canReturn(equipment: Equipment?, uid: String): ReturnCheck = when {
        uid.isBlank() -> ReturnCheck.NOT_SIGNED_IN
        equipment == null -> ReturnCheck.NOT_FOUND
        !equipment.isBorrowed -> ReturnCheck.NOT_BORROWED
        !equipment.isBorrowedBy(uid) -> ReturnCheck.NOT_YOURS
        else -> ReturnCheck.OK
    }

    /** Field update applied to `equipment/{box}` when [uid] takes the item. */
    fun borrowUpdate(uid: String, userName: String, now: Long): Map<String, Any> = mapOf(
        "status" to Equipment.STATUS_BORROWED,
        "borrowedBy" to uid,
        "borrowedByName" to userName,
        "borrowedAt" to now,
        "dueAt" to dueAtFor(now)
    )

    /** Field update applied to `equipment/{box}` when the item goes back in its box. */
    fun returnUpdate(): Map<String, Any> = mapOf(
        "status" to Equipment.STATUS_AVAILABLE,
        "borrowedBy" to "",
        "borrowedByName" to "",
        "borrowedAt" to 0L,
        "dueAt" to 0L
    )

    /** New `transactions/{autoId}` document for a borrow or return event. */
    fun transactionDoc(
        equipment: Equipment,
        uid: String,
        userName: String,
        type: String,
        now: Long
    ): Map<String, Any> = mapOf(
        "uid" to uid,
        "userName" to userName,
        "equipmentId" to equipment.id,
        "equipmentName" to equipment.name,
        "boxNumber" to equipment.boxNumber,
        "type" to type,
        "timestamp" to now
    )

    /** Inbox entry confirming a borrow (FR-05) or a return. */
    fun eventNotification(
        equipment: Equipment,
        uid: String,
        type: String,
        borrowedAt: Long,
        now: Long
    ): AppNotification {
        val name = equipment.name.ifBlank { "Equipment" }
        val title: String
        val body: String
        if (type == Transaction.TYPE_BORROW) {
            title = "Borrowed $name"
            body = "Taken from Box ${equipment.boxNumber}. Please return it by " +
                    "${TimeFormat.dateTime(dueAtFor(now))}."
        } else {
            title = "Returned $name"
            body = "Placed back in Box ${equipment.boxNumber}. Thanks for returning it."
        }
        return AppNotification(
            id = AppNotification.docId(uid, equipment.id, borrowedAt, type),
            uid = uid,
            type = type,
            title = title,
            body = body,
            equipmentId = equipment.id,
            equipmentName = equipment.name,
            boxNumber = equipment.boxNumber,
            read = false,
            createdAt = now
        )
    }

    /**
     * Which FR-06 reminder a loan currently warrants, or null when none is due yet.
     * Only the signed-in borrower's own active loans should be passed in.
     */
    fun pendingReminderType(equipment: Equipment, now: Long): String? {
        if (!equipment.isBorrowed || equipment.dueAt <= 0L) return null
        return when (TimeFormat.dueState(equipment.dueAt, now)) {
            TimeFormat.DueState.OVERDUE -> AppNotification.TYPE_OVERDUE
            TimeFormat.DueState.DUE_SOON -> AppNotification.TYPE_DUE_SOON
            else -> null
        }
    }

    /** Inbox entry (and system notification content) for a due-soon / overdue reminder. */
    fun reminderNotification(
        equipment: Equipment,
        uid: String,
        type: String,
        now: Long
    ): AppNotification {
        val name = equipment.name.ifBlank { "Equipment" }
        val title: String
        val body: String
        if (type == AppNotification.TYPE_OVERDUE) {
            title = "Overdue: $name"
            body = "${TimeFormat.dueLabel(equipment.dueAt, now)}. Return it to Box " +
                    "${equipment.boxNumber} as soon as possible."
        } else {
            title = "Due soon: $name"
            body = "Due at ${TimeFormat.dateTime(equipment.dueAt)}. Please return it to Box " +
                    "${equipment.boxNumber}."
        }
        return AppNotification(
            id = AppNotification.docId(uid, equipment.id, equipment.borrowedAt, type),
            uid = uid,
            type = type,
            title = title,
            body = body,
            equipmentId = equipment.id,
            equipmentName = equipment.name,
            boxNumber = equipment.boxNumber,
            read = false,
            createdAt = now
        )
    }
}
