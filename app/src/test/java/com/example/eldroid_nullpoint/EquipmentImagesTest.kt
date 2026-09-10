package com.example.eldroid_nullpoint

import com.example.eldroid_nullpoint.util.EquipmentImages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies which `equip_<slug>` drawable each piece of equipment maps to.
 * The resource lookup itself needs a Context, so only the keyword matching is
 * covered here - that is where the interesting behaviour lives.
 */
class EquipmentImagesTest {

    private fun slug(name: String, category: String = "") =
        EquipmentImages.slugFor(name, category)

    @Test
    fun `maps the common school equipment names`() {
        assertEquals("projector", slug("Epson LCD Projector", "Audio Visual"))
        assertEquals("microphone", slug("Wireless Microphone Set", "Audio Visual"))
        assertEquals("speaker", slug("Portable Bluetooth Speaker", "Audio Visual"))
        assertEquals("laptop", slug("Dell Laptop", "IT Equipment"))
        assertEquals("cable", slug("HDMI Cable", "Accessories"))
        assertEquals("extension_cord", slug("Extension Cord", "Accessories"))
        assertEquals("camera", slug("DSLR Camera", "Audio Visual"))
        assertEquals("tripod", slug("Camera Tripod", "Accessories"))
        assertEquals("tablet", slug("Android Tablet", "IT Equipment"))
        assertEquals("printer", slug("Canon Printer", "IT Equipment"))
    }

    @Test
    fun `matches short keywords as whole words only`() {
        // "Microscope" contains "mic" but must not be treated as a microphone.
        assertNull(slug("Digital Microscope", "Science"))
        assertEquals("microphone", slug("Handheld Mic", "Audio Visual"))
    }

    @Test
    fun `is case and punctuation insensitive`() {
        assertEquals("projector", slug("PROJECTOR", ""))
        assertEquals("cable", slug("hdmi-cable", ""))
        assertEquals("microphone", slug("MICROPHONE", ""))
    }

    @Test
    fun `has no slug for equipment too large for a SmartDock box`() {
        // A TV is not lendable through the tower, so it must not resolve to an
        // image - it falls through to the placeholder like any unknown item.
        assertNull(slug("55\" Smart TV", "Audio Visual"))
        assertNull(slug("Television", ""))
        assertNull(slug("Computer Monitor", "IT Equipment"))
    }

    @Test
    fun `falls back to no slug for unknown equipment`() {
        assertNull(slug("Chess Board", "Recreation"))
        assertNull(slug("", ""))
    }

    @Test
    fun `can match on category alone`() {
        assertEquals("laptop", slug("Unit 14", "Laptop"))
    }

    @Test
    fun `prefers the specific match over the generic cable catch-all`() {
        // "Extension Cord" contains "cord", which is also a cable keyword.
        assertEquals("extension_cord", slug("Extension Cord", "Accessories"))
    }
}
