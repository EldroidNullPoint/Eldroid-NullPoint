package com.example.eldroid_nullpoint.register

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterPresenterTest {

    private class FakeView : RegisterContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var firstNameError: String? = null
        var lastNameError: String? = null
        var emailError: String? = null
        var passwordError: String? = null
        var confirmPasswordError: String? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun clearFieldErrors() {
            events += "clearFieldErrors"
            firstNameError = null; lastNameError = null; emailError = null
            passwordError = null; confirmPasswordError = null
        }
        override fun showFirstNameError(message: String) { firstNameError = message }
        override fun showLastNameError(message: String) { lastNameError = message }
        override fun showEmailError(message: String) { emailError = message }
        override fun showPasswordError(message: String) { passwordError = message }
        override fun showConfirmPasswordError(message: String) { confirmPasswordError = message }
        override fun launchGoogleSignIn() { events += "launchGoogleSignIn" }
        override fun navigateToHome() { events += "navigateToHome" }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    private lateinit var repository: FakeAuthRepository
    private lateinit var view: FakeView
    private lateinit var presenter: RegisterPresenter

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        view = FakeView()
        presenter = RegisterPresenter(repository)
        presenter.attachView(view)
    }

    @Test
    fun `all fields are validated at once`() {
        presenter.onRegisterClicked(
            firstName = "",
            lastName = "D",
            email = "bad",
            password = "weak",
            confirmPassword = ""
        )

        assertEquals("First name is required", view.firstNameError)
        assertEquals("Last name must be at least 2 characters", view.lastNameError)
        assertEquals("Enter a valid email address", view.emailError)
        assertEquals("Password must be at least 10 characters", view.passwordError)
        assertEquals("Please confirm your password", view.confirmPasswordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `password containing the user's name is rejected`() {
        presenter.onRegisterClicked("Jane", "Doe", "jane@example.com", "Janexyq!19Q", "Janexyq!19Q")

        assertEquals("Password must not contain your name or email", view.passwordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `mismatched confirmation is rejected`() {
        presenter.onRegisterClicked("Jane", "Doe", "jane@example.com", "Tr0ub4dor&3x", "Tr0ub4dor&3X")

        assertNull(view.passwordError)
        assertEquals("Passwords do not match", view.confirmPasswordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `valid form registers with cleaned values and navigates home`() {
        presenter.onRegisterClicked(
            firstName = "  Mary   Ann ",
            lastName = " O'Neil ",
            email = " mary@example.com ",
            password = "Tr0ub4dor&3x",
            confirmPassword = "Tr0ub4dor&3x"
        )

        assertEquals(listOf("Mary Ann", "O'Neil", "mary@example.com", "Tr0ub4dor&3x"), repository.lastRegisterArgs)
        assertEquals(listOf("clearFieldErrors", "showLoading", "hideLoading", "navigateToHome"), view.events)
    }

    @Test
    fun `duplicate account shows the repository message`() {
        repository.registerResult = Result.failure(AuthError("An account already exists with this email."))

        presenter.onRegisterClicked("Jane", "Doe", "jane@example.com", "Tr0ub4dor&3x", "Tr0ub4dor&3x")

        assertEquals(listOf("An account already exists with this email."), view.messages)
        assertTrue("navigateToHome" !in view.events)
    }

    @Test
    fun `google sign up flows through the repository`() {
        presenter.onGoogleSignUpClicked()
        presenter.onGoogleSignInResult("tok")

        assertEquals("tok", repository.lastGoogleToken)
        assertEquals(listOf("showLoading", "launchGoogleSignIn", "hideLoading", "navigateToHome"), view.events)
    }

    @Test
    fun `google failure reports reason`() {
        presenter.onGoogleSignUpClicked()
        presenter.onGoogleSignInFailed("10: DEVELOPER_ERROR")

        assertEquals(listOf("Google sign-in failed: 10: DEVELOPER_ERROR"), view.messages)
        assertTrue("hideLoading" in view.events)
    }

    @Test
    fun `login link navigates`() {
        presenter.onLoginClicked()
        assertEquals(listOf("navigateToLogin"), view.events)
    }
}
