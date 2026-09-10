package com.example.eldroid_nullpoint.util

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction
import java.util.concurrent.TimeUnit

/**
 * Built-in placeholder content so the dashboard has something to show before the
 * SmartDock tower and its Firestore documents exist.
 *
 * This is a *display* fallback only. Nothing here is ever written to Firestore, so
 * it cannot pollute real data, and it is used only when the live query comes back
 * with nothing (or cannot be read at all). The moment real equipment documents
 * exist, they win and the demo content disappears on its own.
 *
 * To turn it off completely for the final build, set [ENABLED] to false - that is
 * the single switch, no other file needs touching.
 */
object DemoData {

    /** Master switch for the placeholder dashboard content. */
    const val ENABLED = true

    private const val DEMO_ID_PREFIX = "demo_box_"

    fun isDemoId(id: String): Boolean = id.startsWith(DEMO_ID_PREFIX)

    private fun hoursAgo(hours: Long) =
        System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours)

    private fun hoursFromNow(hours: Long) =
        System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hours)

    /**
     * Ten boxes covering every borrower-facing state: available, borrowed by
     * someone else, borrowed by [currentUid], and overdue. Times are computed
     * relative to now, so the countdowns always read sensibly no matter when the
     * app is opened.
     */
    fun equipment(currentUid: String): List<Equipment> {
        val me = currentUid.ifBlank { "demo-user" }
        return listOf(
            Equipment(
                id = "${DEMO_ID_PREFIX}01",
                name = "Epson LCD Projector",
                category = "Audio Visual",
                boxNumber = 1,
                status = Equipment.STATUS_BORROWED,
                borrowedBy = me,
                borrowedByName = "You",
                borrowedAt = hoursAgo(2),
                dueAt = hoursFromNow(4)
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}02",
                name = "Wireless Microphone Set",
                category = "Audio Visual",
                boxNumber = 2
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}03",
                name = "Portable Bluetooth Speaker",
                category = "Audio Visual",
                boxNumber = 3,
                status = Equipment.STATUS_BORROWED,
                borrowedBy = "another-borrower",
                borrowedByName = "Another borrower",
                borrowedAt = hoursAgo(5),
                dueAt = hoursFromNow(2)
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}04",
                name = "Dell Laptop",
                category = "IT Equipment",
                boxNumber = 4
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}05",
                name = "HDMI Cable",
                category = "Accessories",
                boxNumber = 5
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}06",
                name = "Extension Cord",
                category = "Accessories",
                boxNumber = 6
            ),
            // Deliberately overdue, so the red overdue treatment is visible.
            Equipment(
                id = "${DEMO_ID_PREFIX}07",
                name = "DSLR Camera",
                category = "Audio Visual",
                boxNumber = 7,
                status = Equipment.STATUS_BORROWED,
                borrowedBy = me,
                borrowedByName = "You",
                borrowedAt = hoursAgo(30),
                dueAt = hoursAgo(6)
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}08",
                name = "Camera Tripod",
                category = "Accessories",
                boxNumber = 8
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}09",
                name = "Android Tablet",
                category = "IT Equipment",
                boxNumber = 9,
                status = Equipment.STATUS_BORROWED,
                borrowedBy = "another-borrower",
                borrowedByName = "Another borrower",
                borrowedAt = hoursAgo(8),
                dueAt = hoursFromNow(16)
            ),
            Equipment(
                id = "${DEMO_ID_PREFIX}10",
                name = "Portable Printer",
                category = "IT Equipment",
                boxNumber = 10
            )
        )
    }

    fun equipmentById(id: String, currentUid: String): Equipment? =
        equipment(currentUid).firstOrNull { it.id == id }

    /** Six history entries - enough that "See all" appears on the dashboard. */
    fun transactions(currentUid: String, userName: String): List<Transaction> {
        val me = currentUid.ifBlank { "demo-user" }
        val who = userName.ifBlank { "Borrower" }
        return listOf(
            Transaction(
                id = "demo_txn_01", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}01", equipmentName = "Epson LCD Projector",
                boxNumber = 1, type = Transaction.TYPE_BORROW, timestamp = hoursAgo(2)
            ),
            Transaction(
                id = "demo_txn_02", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}07", equipmentName = "DSLR Camera",
                boxNumber = 7, type = Transaction.TYPE_OVERDUE, timestamp = hoursAgo(6)
            ),
            Transaction(
                id = "demo_txn_03", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}07", equipmentName = "DSLR Camera",
                boxNumber = 7, type = Transaction.TYPE_BORROW, timestamp = hoursAgo(30)
            ),
            Transaction(
                id = "demo_txn_04", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}05", equipmentName = "HDMI Cable",
                boxNumber = 5, type = Transaction.TYPE_RETURN, timestamp = hoursAgo(28)
            ),
            Transaction(
                id = "demo_txn_05", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}05", equipmentName = "HDMI Cable",
                boxNumber = 5, type = Transaction.TYPE_BORROW, timestamp = hoursAgo(34)
            ),
            Transaction(
                id = "demo_txn_06", uid = me, userName = who,
                equipmentId = "${DEMO_ID_PREFIX}02", equipmentName = "Wireless Microphone Set",
                boxNumber = 2, type = Transaction.TYPE_RETURN, timestamp = hoursAgo(50)
            )
        )
    }
}
