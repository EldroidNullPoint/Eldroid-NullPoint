package com.example.eldroid_nullpoint

import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The placeholder dashboard has to demonstrate every borrower-facing state, so
 * these assertions pin down the shape of the sample set.
 */
class DemoDataTest {

    private val uid = "test-uid-123"

    @Test
    fun `provides ten boxes with unique ids and box numbers`() {
        val equipment = DemoData.equipment(uid)
        assertEquals(10, equipment.size)
        assertEquals(10, equipment.map { it.id }.toSet().size)
        assertEquals((1..10).toList(), equipment.map { it.boxNumber }.sorted())
        assertTrue(equipment.all { it.name.isNotBlank() && it.category.isNotBlank() })
    }

    @Test
    fun `covers every borrower-facing status`() {
        val equipment = DemoData.equipment(uid)

        val mine = equipment.filter { it.isBorrowedBy(uid) }
        val available = equipment.filter { !it.isBorrowed }
        val someoneElse = equipment.filter { it.isBorrowed && !it.isBorrowedBy(uid) }

        // Two of mine, so the hero card AND the "My Borrowed Items" list both show.
        assertEquals(2, mine.size)
        assertEquals(6, available.size)
        assertEquals(2, someoneElse.size)

        // One of mine is overdue (red treatment), the other is still running.
        assertEquals(1, mine.count { it.isOverdue() })
        assertEquals(1, mine.count { !it.isOverdue() })
    }

    @Test
    fun `borrowed items carry sensible borrow and due times`() {
        val now = System.currentTimeMillis()
        DemoData.equipment(uid).filter { it.isBorrowed }.forEach { item ->
            assertTrue("${item.name} should have a borrow time", item.borrowedAt > 0L)
            assertTrue("${item.name} should have a due time", item.dueAt > 0L)
            assertTrue("${item.name} borrowed in the past", item.borrowedAt < now)
            assertTrue("${item.name} due after borrow", item.dueAt > item.borrowedAt)
        }
    }

    @Test
    fun `available items carry no borrower or times`() {
        DemoData.equipment(uid).filter { !it.isBorrowed }.forEach { item ->
            assertTrue(item.borrowedBy.isEmpty())
            assertEquals(0L, item.borrowedAt)
            assertEquals(0L, item.dueAt)
            assertFalse(item.isOverdue())
        }
    }

    @Test
    fun `history is attributed to the borrower and covers each event type`() {
        val transactions = DemoData.transactions(uid, "Bryce Mendez")

        assertEquals(6, transactions.size)
        assertTrue(transactions.all { it.uid == uid })
        assertTrue(transactions.all { it.timestamp > 0L })
        assertTrue(transactions.any { it.type == Transaction.TYPE_BORROW })
        assertTrue(transactions.any { it.type == Transaction.TYPE_RETURN })
        assertTrue(transactions.any { it.type == Transaction.TYPE_OVERDUE })

        // More than the dashboard's 4-row preview, so "See all" is reachable.
        assertTrue(transactions.size > 4)
    }

    @Test
    fun `history rows point at real demo equipment`() {
        val ids = DemoData.equipment(uid).map { it.id }.toSet()
        DemoData.transactions(uid, "Bryce Mendez").forEach {
            assertTrue("${it.equipmentName} should link to a demo box", it.equipmentId in ids)
        }
    }

    @Test
    fun `demo ids are recognised and resolvable`() {
        val first = DemoData.equipment(uid).first()
        assertTrue(DemoData.isDemoId(first.id))
        assertNotNull(DemoData.equipmentById(first.id, uid))

        assertFalse(DemoData.isDemoId("box_01"))
        assertNull(DemoData.equipmentById("box_01", uid))
    }

    @Test
    fun `falls back to a stand-in uid when signed out`() {
        val equipment = DemoData.equipment("")
        assertTrue(equipment.any { it.isBorrowed })
        assertTrue(DemoData.transactions("", "").all { it.uid.isNotBlank() })
    }
}
