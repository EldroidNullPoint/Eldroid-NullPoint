package com.example.eldroid_nullpoint

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.SeedEquipment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sample boxes written by "Add sample equipment" must match the document
 * contract in firestore/equipment.seed.json and the security rules' create check.
 */
class SeedEquipmentTest {

    @Test
    fun `seeds ten boxes with tower-style ids and unique box numbers`() {
        val boxes = SeedEquipment.BOXES
        assertEquals(10, boxes.size)
        assertEquals((1..10).map { "box_" + it.toString().padStart(2, '0') }, boxes.map { it.id })
        assertEquals((1..10).toList(), boxes.map { it.boxNumber })
        assertTrue(boxes.all { it.name.isNotBlank() && it.category.isNotBlank() })
    }

    @Test
    fun `every seeded box starts available with no loan data`() {
        SeedEquipment.BOXES.forEach { box ->
            val doc = SeedEquipment.toMap(box)
            assertEquals(Equipment.STATUS_AVAILABLE, doc["status"])
            assertEquals("", doc["borrowedBy"])
            assertEquals("", doc["borrowedByName"])
            assertEquals(0L, doc["borrowedAt"])
            assertEquals(0L, doc["dueAt"])
            assertEquals(box.boxNumber, doc["boxNumber"])
            assertFalse("id must be the document key, not a field", doc.containsKey("id"))
        }
    }

    @Test
    fun `every seeded item has a photo`() {
        SeedEquipment.BOXES.forEach { box ->
            assertNotNull("No thumbnail slug for ${box.name}", EquipmentImages.slugFor(box.name, box.category))
        }
    }
}
