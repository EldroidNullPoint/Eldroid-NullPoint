package com.example.eldroid_nullpoint

import com.example.eldroid_nullpoint.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the password and name rules used by Register and Change Password.
 *
 * Email validation is not covered here: it delegates to android.util.Patterns,
 * which is not available to plain JVM unit tests.
 */
class ValidatorsTest {

    @Test
    fun `accepts a password meeting every requirement`() {
        assertTrue(Validators.isStrongPassword("SmartDock1!"))
        assertNull(Validators.passwordStrengthError("SmartDock1!"))
    }

    @Test
    fun `rejects passwords shorter than eight characters`() {
        assertFalse(Validators.isStrongPassword("Sd1!abc"))
        assertEquals(
            "Password must be at least 8 characters",
            Validators.passwordStrengthError("Sd1!abc")
        )
    }

    @Test
    fun `rejects passwords missing a character class`() {
        assertFalse(Validators.isStrongPassword("smartdock1!")) // no uppercase
        assertFalse(Validators.isStrongPassword("SMARTDOCK1!")) // no lowercase
        assertFalse(Validators.isStrongPassword("SmartDock!"))  // no digit
        assertFalse(Validators.isStrongPassword("SmartDock11")) // no special char

        assertEquals(
            "Add at least one uppercase letter",
            Validators.passwordStrengthError("smartdock1!")
        )
        assertEquals(
            "Add at least one lowercase letter",
            Validators.passwordStrengthError("SMARTDOCK1!")
        )
        assertEquals("Add at least one number", Validators.passwordStrengthError("SmartDock!"))
        assertEquals(
            "Add at least one special character",
            Validators.passwordStrengthError("SmartDock11")
        )
    }

    @Test
    fun `rejects passwords containing spaces`() {
        assertFalse(Validators.isStrongPassword("Smart Dock1!"))
        assertEquals(
            "Password cannot contain spaces",
            Validators.passwordStrengthError("Smart Dock1!")
        )
    }

    @Test
    fun `rejects a blank password`() {
        assertFalse(Validators.isStrongPassword(""))
        assertEquals("Password is required", Validators.passwordStrengthError(""))
        assertNotNull(Validators.passwordStrengthError("   "))
    }

    @Test
    fun `accepts ordinary names and trims surrounding whitespace`() {
        assertTrue(Validators.isValidName("Bryce"))
        assertTrue(Validators.isValidName("  Mendez  "))
        assertTrue(Validators.isValidName("O'Brien"))
        assertTrue(Validators.isValidName("Anne-Marie"))
    }

    @Test
    fun `rejects names that are blank, too short, or have no letters`() {
        assertFalse(Validators.isValidName(""))
        assertFalse(Validators.isValidName("   "))
        assertFalse(Validators.isValidName("B"))
        assertFalse(Validators.isValidName("12345"))
        assertFalse(Validators.isValidName("---"))
        assertFalse(Validators.isValidName("!@#"))
    }

    @Test
    fun `accepts RFID card UIDs in the formats a reader prints`() {
        assertTrue(Validators.isValidRfidUid("04 A3 2B 1C"))
        assertTrue(Validators.isValidRfidUid("04:A3:2B:1C"))
        assertTrue(Validators.isValidRfidUid("04A32B1C"))
        assertTrue(Validators.isValidRfidUid("  1a2b3c4d  "))
        // Blank means "no card linked yet" and is allowed.
        assertTrue(Validators.isValidRfidUid(""))
        assertTrue(Validators.isValidRfidUid("   "))
    }

    @Test
    fun `rejects RFID card UIDs that are too short, too long or contain symbols`() {
        assertFalse(Validators.isValidRfidUid("04A"))
        assertFalse(Validators.isValidRfidUid("A".repeat(33)))
        assertFalse(Validators.isValidRfidUid("04_A3_2B"))
        assertFalse(Validators.isValidRfidUid("04.A3.2B"))
        assertFalse(Validators.isValidRfidUid("----"))
    }
}
