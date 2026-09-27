package com.example.eldroid_nullpoint.util

import com.example.eldroid_nullpoint.model.Equipment

/**
 * The ten sample SmartDock boxes, mirrored from `firestore/equipment.seed.json`.
 *
 * The dashboard offers a one-tap "Add sample equipment" action while the
 * `equipment` collection is still empty, so the tower can be demonstrated before
 * the hardware exists. Document ids are the box ids (`box_01`..`box_10`), matching
 * what the administrator side / ESP32 would create.
 */
object SeedEquipment {

    val BOXES: List<Equipment> = listOf(
        box(1, "Epson LCD Projector", "Audio Visual"),
        box(2, "Wireless Microphone Set", "Audio Visual"),
        box(3, "Portable Bluetooth Speaker", "Audio Visual"),
        box(4, "Dell Laptop", "IT Equipment"),
        box(5, "HDMI Cable", "Accessories"),
        box(6, "Extension Cord", "Accessories"),
        box(7, "DSLR Camera", "Audio Visual"),
        box(8, "Camera Tripod", "Accessories"),
        box(9, "Android Tablet", "IT Equipment"),
        box(10, "Portable Printer", "IT Equipment")
    )

    private fun box(number: Int, name: String, category: String) = Equipment(
        id = "box_" + number.toString().padStart(2, '0'),
        name = name,
        category = category,
        boxNumber = number
    )

    /** Document body for a fresh, available box (the id becomes the document id). */
    fun toMap(equipment: Equipment): Map<String, Any> = mapOf(
        "name" to equipment.name,
        "category" to equipment.category,
        "boxNumber" to equipment.boxNumber,
        "status" to Equipment.STATUS_AVAILABLE,
        "borrowedBy" to "",
        "borrowedByName" to "",
        "borrowedAt" to 0,   // Int 0 — matches Firestore rule: incoming().borrowedAt == 0
        "dueAt" to 0         // Int 0 — matches Firestore rule: incoming().dueAt == 0
    )
}
