package com.example.eldroid_nullpoint

import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.LoanPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * The borrow/return rules and the document shapes they produce. The write maps
 * are what `firestore.rules` validates, so these tests pin them down.
 */
class LoanPolicyTest {

    private val me = "uid-me"
    private val other = "uid-other"
    private val now = 1_800_000_000_000L

    private val available = Equipment(id = "box_01", name = "Epson LCD Projector", boxNumber = 1)
    private val mine = available.copy(
        status = Equipment.STATUS_BORROWED,
        borrowedBy = me,
        borrowedByName = "Me",
        borrowedAt = now - TimeUnit.HOURS.toMillis(2),
        dueAt = now + TimeUnit.HOURS.toMillis(22)
    )
    private val theirs = mine.copy(borrowedBy = other, borrowedByName = "Someone")

    @Test
    fun `loans are due exactly 24 hours after they start`() {
        assertEquals(TimeUnit.HOURS.toMillis(24), LoanPolicy.LOAN_DURATION_MS)
        assertEquals(now + TimeUnit.HOURS.toMillis(24), LoanPolicy.dueAtFor(now))
    }

    @Test
    fun `canBorrow only passes for an available box and a signed-in borrower`() {
        assertEquals(LoanPolicy.BorrowCheck.OK, LoanPolicy.canBorrow(available, me))
        assertEquals(LoanPolicy.BorrowCheck.ALREADY_BORROWED, LoanPolicy.canBorrow(theirs, me))
        assertEquals(LoanPolicy.BorrowCheck.ALREADY_BORROWED, LoanPolicy.canBorrow(mine, me))
        assertEquals(LoanPolicy.BorrowCheck.NOT_FOUND, LoanPolicy.canBorrow(null, me))
        assertEquals(LoanPolicy.BorrowCheck.NOT_SIGNED_IN, LoanPolicy.canBorrow(available, ""))
    }

    @Test
    fun `canReturn only passes for the borrower holding the item`() {
        assertEquals(LoanPolicy.ReturnCheck.OK, LoanPolicy.canReturn(mine, me))
        assertEquals(LoanPolicy.ReturnCheck.NOT_YOURS, LoanPolicy.canReturn(theirs, me))
        assertEquals(LoanPolicy.ReturnCheck.NOT_BORROWED, LoanPolicy.canReturn(available, me))
        assertEquals(LoanPolicy.ReturnCheck.NOT_FOUND, LoanPolicy.canReturn(null, me))
        assertEquals(LoanPolicy.ReturnCheck.NOT_SIGNED_IN, LoanPolicy.canReturn(mine, ""))
    }

    @Test
    fun `borrowUpdate writes exactly the fields the security rules expect`() {
        val update = LoanPolicy.borrowUpdate(me, "Me", now)
        assertEquals(
            setOf("status", "borrowedBy", "borrowedByName", "borrowedAt", "dueAt"),
            update.keys
        )
        assertEquals(Equipment.STATUS_BORROWED, update["status"])
        assertEquals(me, update["borrowedBy"])
        assertEquals("Me", update["borrowedByName"])
        assertEquals(now, update["borrowedAt"])
        assertTrue((update["dueAt"] as Long) > now)
    }

    @Test
    fun `returnUpdate clears every loan field`() {
        val update = LoanPolicy.returnUpdate()
        assertEquals(Equipment.STATUS_AVAILABLE, update["status"])
        assertEquals("", update["borrowedBy"])
        assertEquals("", update["borrowedByName"])
        assertEquals(0L, update["borrowedAt"])
        assertEquals(0L, update["dueAt"])
    }

    @Test
    fun `transactionDoc carries the borrower, the box and the event`() {
        val doc = LoanPolicy.transactionDoc(available, me, "Me", Transaction.TYPE_BORROW, now)
        assertEquals(me, doc["uid"])
        assertEquals("Me", doc["userName"])
        assertEquals("box_01", doc["equipmentId"])
        assertEquals("Epson LCD Projector", doc["equipmentName"])
        assertEquals(1, doc["boxNumber"])
        assertEquals(Transaction.TYPE_BORROW, doc["type"])
        assertEquals(now, doc["timestamp"])
    }

    @Test
    fun `reminder type follows the due state of the loan`() {
        val dueSoon = mine.copy(dueAt = now + TimeUnit.MINUTES.toMillis(30))
        val overdue = mine.copy(dueAt = now - TimeUnit.MINUTES.toMillis(1))
        val later = mine.copy(dueAt = now + TimeUnit.HOURS.toMillis(5))

        assertEquals(AppNotification.TYPE_DUE_SOON, LoanPolicy.pendingReminderType(dueSoon, now))
        assertEquals(AppNotification.TYPE_OVERDUE, LoanPolicy.pendingReminderType(overdue, now))
        assertNull(LoanPolicy.pendingReminderType(later, now))
        assertNull(LoanPolicy.pendingReminderType(available, now))
        assertNull(LoanPolicy.pendingReminderType(mine.copy(dueAt = 0L), now))
    }

    @Test
    fun `notification ids are deterministic per loan and type`() {
        val a = LoanPolicy.reminderNotification(mine, me, AppNotification.TYPE_DUE_SOON, now)
        val again = LoanPolicy.reminderNotification(mine, me, AppNotification.TYPE_DUE_SOON, now + 1)
        val overdue = LoanPolicy.reminderNotification(mine, me, AppNotification.TYPE_OVERDUE, now)
        val nextLoan = LoanPolicy.reminderNotification(
            mine.copy(borrowedAt = mine.borrowedAt + 1), me, AppNotification.TYPE_DUE_SOON, now
        )

        assertEquals(a.id, again.id)
        assertNotEquals(a.id, overdue.id)
        assertNotEquals(a.id, nextLoan.id)
        assertEquals(AppNotification.docId(me, "box_01", mine.borrowedAt, AppNotification.TYPE_DUE_SOON), a.id)
        assertFalse(a.read)
        assertTrue(a.title.contains("Epson LCD Projector"))
        assertTrue(overdue.title.startsWith("Overdue"))
    }

    @Test
    fun `borrow confirmation mentions the box and the due time`() {
        val n = LoanPolicy.eventNotification(available, me, AppNotification.TYPE_BORROW, now, now)
        assertEquals(AppNotification.TYPE_BORROW, n.type)
        assertEquals("box_01", n.equipmentId)
        assertEquals(1, n.boxNumber)
        assertTrue(n.body.contains("Box 1"))
        assertEquals(me, n.toMap()["uid"])
        assertEquals(false, n.toMap()["read"])
    }
}
