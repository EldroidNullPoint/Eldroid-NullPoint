package com.example.eldroid_nullpoint.util

import com.example.eldroid_nullpoint.util.Validators.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    // ---------------------------------------------------------------
    // Email
    // ---------------------------------------------------------------

    @Test
    fun `valid emails pass`() {
        listOf(
            "jane.doe@example.com",
            "a+b@sub.domain.co",
            "  padded@example.org  ",
            "first_last-99@my-host.io"
        ).forEach { assertNull("expected valid: $it", Validators.emailError(it)) }
    }

    @Test
    fun `empty email is required`() {
        assertEquals("Email is required", Validators.emailError("   "))
    }

    @Test
    fun `malformed emails fail`() {
        listOf("plain", "a@b", "a@b.c", "@example.com", "jane@", "jane doe@example.com", "jane@exa mple.com")
            .forEach { assertNotNull("expected invalid: $it", Validators.emailError(it)) }
    }

    @Test
    fun `consecutive or edge dots fail`() {
        assertEquals("Email cannot contain consecutive dots", Validators.emailError("a..b@example.com"))
        assertNotNull(Validators.emailError(".jane@example.com"))
        assertNotNull(Validators.emailError("jane.@example.com"))
    }

    @Test
    fun `overlong email fails`() {
        val local = "a".repeat(250)
        assertEquals("Email is too long", Validators.emailError("$local@example.com"))
    }

    // ---------------------------------------------------------------
    // Names
    // ---------------------------------------------------------------

    @Test
    fun `valid names pass`() {
        listOf("Jo", "Mary-Ann", "O'Neil", "Jean Luc", "José", "Nguyễn")
            .forEach { assertNull("expected valid: $it", Validators.nameError(it, "First name")) }
    }

    @Test
    fun `name errors use the field label`() {
        assertEquals("Last name is required", Validators.nameError("", "Last name"))
        assertEquals("First name must be at least 2 characters", Validators.nameError("J", "First name"))
        assertEquals("First name must be 40 characters or fewer", Validators.nameError("a".repeat(41), "First name"))
        assertEquals("First name cannot contain numbers", Validators.nameError("J4ne", "First name"))
    }

    @Test
    fun `names reject symbols and double spaces`() {
        assertNotNull(Validators.nameError("Jane  Doe", "First name"))
        assertNotNull(Validators.nameError("Jane!", "First name"))
        assertNotNull(Validators.nameError("-Jane", "First name"))
        assertNotNull(Validators.nameError("Jane-", "First name"))
    }

    // ---------------------------------------------------------------
    // Password rules
    // ---------------------------------------------------------------

    @Test
    fun `strong passwords pass`() {
        listOf("Tr0ub4dor&3x", "Correct-Horse9!", "Mxq7#Lpz2Rw", "G!ant.Ste9pLaddEr")
            .forEach { assertNull("expected strong: $it", Validators.passwordError(it)) }
    }

    @Test
    fun `password presence and length`() {
        assertEquals("Password is required", Validators.passwordError(""))
        assertEquals("Password cannot contain spaces", Validators.passwordError("Ab1! def ghij"))
        assertEquals("Password must be at least 10 characters", Validators.passwordError("Ab1!xyzq"))
        assertEquals(
            "Password must be 64 characters or fewer",
            Validators.passwordError("Ab1!" + "xq".repeat(31))
        )
    }

    @Test
    fun `password character classes`() {
        assertEquals("Add at least one uppercase letter", Validators.passwordError("kmpqrtvw1!"))
        assertEquals("Add at least one lowercase letter", Validators.passwordError("KMPQRTVW1!"))
        assertEquals("Add at least one number", Validators.passwordError("Kmpqrtvw!!"))
        assertEquals("Add at least one special character (e.g. ! @ # \$ %)", Validators.passwordError("Kmpqrtvw11"))
    }

    @Test
    fun `password rejects repeated runs`() {
        assertEquals(
            "Password cannot repeat the same character 3 or more times in a row",
            Validators.passwordError("Kmpaaa1!vw")
        )
        assertNull(Validators.passwordError("Kmpaa1!vwQ")) // two in a row is fine
    }

    @Test
    fun `password rejects sequences`() {
        val expected = "Password cannot contain sequences like 1234, abcd or qwer"
        assertEquals(expected, Validators.passwordError("Km1234pq!X"))
        assertEquals(expected, Validators.passwordError("Kmabcdq!1X"))
        assertEquals(expected, Validators.passwordError("Km4321pq!X"))
        assertEquals(expected, Validators.passwordError("Kmqwerp!1X"))
        assertEquals(expected, Validators.passwordError("KmASDFp!1x"))
    }

    @Test
    fun `password rejects common words even in leet speak`() {
        val expected = "This password is too common or easy to guess"
        assertEquals(expected, Validators.passwordError("P@ssw0rd!Xk"))
        assertEquals(expected, Validators.passwordError("L3tMe1n!!Xk"))
        assertEquals(expected, Validators.passwordError("Xk!9Welcome"))
    }

    @Test
    fun `password must not contain personal info`() {
        val expected = "Password must not contain your name or email"
        assertEquals(expected, Validators.passwordError("Janexyq!19Q", firstName = "Jane"))
        assertEquals(expected, Validators.passwordError("Kp!9DOE77Qz", lastName = "Doe"))
        assertEquals(expected, Validators.passwordError("Kp!9jdoe77Q", email = "jdoe@example.com"))
        assertEquals(expected, Validators.passwordError("Kp!9ann77Qz", firstName = "Mary Ann"))
        // Short tokens (< 3 chars) are ignored so "Jo" does not block every password with "jo".
        assertNull(Validators.passwordError("Kp!9major7Q", firstName = "Jo"))
    }

    @Test
    fun `confirm password rules`() {
        assertEquals("Please confirm your password", Validators.confirmPasswordError("Tr0ub4dor&3x", ""))
        assertEquals("Passwords do not match", Validators.confirmPasswordError("Tr0ub4dor&3x", "Tr0ub4dor&3X"))
        assertNull(Validators.confirmPasswordError("Tr0ub4dor&3x", "Tr0ub4dor&3x"))
    }

    @Test
    fun `new password must differ from current`() {
        assertEquals(
            "New password must be different from your current password",
            Validators.newPasswordError("Tr0ub4dor&3x", "Tr0ub4dor&3x")
        )
        assertEquals("Add at least one number", Validators.newPasswordError("Tr0ub4dor&3x", "Kmpqrtvw!!"))
        assertNull(Validators.newPasswordError("Tr0ub4dor&3x", "Correct-Horse9!"))
    }

    @Test
    fun `password strength score`() {
        assertEquals(PasswordStrength.EMPTY, Validators.passwordStrength(""))
        assertEquals(PasswordStrength.WEAK, Validators.passwordStrength("abc"))
        assertEquals(PasswordStrength.WEAK, Validators.passwordStrength("password1234"))
        assertEquals(PasswordStrength.WEAK, Validators.passwordStrength("kmpqrtvwxz1"))   // 2 classes
        assertEquals(PasswordStrength.FAIR, Validators.passwordStrength("Kmpqrtvwxz1"))   // 3 classes
        assertEquals(PasswordStrength.STRONG, Validators.passwordStrength("Tr0ub4dor&3x"))
        assertEquals(PasswordStrength.VERY_STRONG, Validators.passwordStrength("Correct-Horse9!Jump"))
    }

    @Test
    fun `sequence helpers`() {
        assertTrue(Validators.hasSequentialRun("xx4321xx"))
        assertTrue(Validators.hasSequentialRun("zxcv"))
        assertFalse(Validators.hasSequentialRun("Tr0ub4dor&3x"))
        assertTrue(Validators.hasRepeatedRun("ab111c"))
        assertFalse(Validators.hasRepeatedRun("ab11c"))
        assertTrue(Validators.isCommonPassword("P@55w0rd"))
        assertFalse(Validators.isCommonPassword("Tr0ub4dor&3x"))
    }
}
