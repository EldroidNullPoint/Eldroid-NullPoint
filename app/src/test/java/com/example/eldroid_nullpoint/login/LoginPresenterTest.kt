package com.example.eldroid_nullpoint.login

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginPresenterTest {

    private class FakeView : LoginContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var emailError: String? = null
        var passwordError: String? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun clearFieldErrors() { events += "clearFieldErrors"; emailError = null; passwordError = null }
        override fun showEmailError(message: String) { events += "showEmailError"; emailError = message }
        override fun showPasswordError(message: String) { events += "showPasswordError"; passwordError = message }
        override fun launchGoogleSignIn() { events += "launchGoogleSignIn" }
        override fun navigateToHome() { events += "navigateToHome" }
        override fun navigateToRegister() { events += "navigateToRegister" }
    }

    private lateinit var repository: FakeAuthRepository
    private lateinit var view: FakeView
    private lateinit var presenter: LoginPresenter

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        view = FakeView()
        presenter = LoginPresenter(repository)
        presenter.attachView(view)
    }

    @Test
    fun `invalid email and blank password show both errors and skip the repository`() {
        presenter.onLoginClicked("not-an-email", "")

        assertEquals("Enter a valid email address", view.emailError)
        assertEquals("Password is required", view.passwordError)
        assertTrue(repository.calls.isEmpty())
        assertTrue("showLoading" !in view.events)
    }

    @Test
    fun `valid credentials show loading then navigate home`() {
        repository.respondImmediately = false

        presenter.onLoginClicked("  jane.doe@example.com ", "Secret!123x")

        assertNull(view.emailError)
        assertEquals(listOf("clearFieldErrors", "showLoading"), view.events)
        assertEquals("jane.doe@example.com", repository.lastLoginEmail)   // trimmed
        assertEquals("Secret!123x", repository.lastLoginPassword)          // never trimmed

        repository.flushPending()
        assertEquals(listOf("clearFieldErrors", "showLoading", "hideLoading", "navigateToHome"), view.events)
    }

    @Test
    fun `failed login shows the repository message`() {
        repository.loginResult = Result.failure(AuthError("Incorrect email or password."))

        presenter.onLoginClicked("jane.doe@example.com", "wrong")

        assertEquals(listOf("Incorrect email or password."), view.messages)
        assertTrue("navigateToHome" !in view.events)
        assertTrue("hideLoading" in view.events)
    }

    @Test
    fun `google sign in launches the picker and completes on token`() {
        presenter.onGoogleSignInClicked()
        assertEquals(listOf("showLoading", "launchGoogleSignIn"), view.events)

        presenter.onGoogleSignInResult("id-token")
        assertEquals("id-token", repository.lastGoogleToken)
        assertTrue("navigateToHome" in view.events)
    }

    @Test
    fun `google sign in without token reports an error`() {
        presenter.onGoogleSignInClicked()
        presenter.onGoogleSignInResult(null)

        assertTrue(repository.calls.isEmpty())
        assertTrue("hideLoading" in view.events)
        assertEquals(1, view.messages.size)
    }

    @Test
    fun `google cancel only hides loading`() {
        presenter.onGoogleSignInClicked()
        presenter.onGoogleSignInCancelled()

        assertEquals(listOf("showLoading", "launchGoogleSignIn", "hideLoading"), view.events)
        assertTrue(view.messages.isEmpty())
    }

    @Test
    fun `forgot password requires a valid email`() {
        presenter.onForgotPasswordClicked("nope")

        assertEquals("Enter a valid email address", view.emailError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `forgot password sends reset and confirms`() {
        presenter.onForgotPasswordClicked(" jane.doe@example.com ")

        assertEquals("jane.doe@example.com", repository.lastResetEmail)
        assertEquals(listOf("Password reset email sent to jane.doe@example.com"), view.messages)
    }

    @Test
    fun `register link navigates`() {
        presenter.onRegisterClicked()
        assertEquals(listOf("navigateToRegister"), view.events)
    }

    @Test
    fun `callbacks after detach do not touch the view`() {
        repository.respondImmediately = false
        presenter.onLoginClicked("jane.doe@example.com", "Secret!123x")
        presenter.detachView()

        repository.flushPending()

        assertTrue("navigateToHome" !in view.events)
        assertTrue("hideLoading" !in view.events)
    }
}
