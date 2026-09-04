package com.example.eldroid_nullpoint.forgotpassword

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForgotPasswordPresenterTest {

    private class FakeView : ForgotPasswordContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var emailError: String? = null
        var sentTo: String? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun clearFieldErrors() { events += "clearFieldErrors"; emailError = null }
        override fun showEmailError(message: String) { events += "showEmailError"; emailError = message }
        override fun showResetEmailSent(email: String) { events += "showResetEmailSent"; sentTo = email }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    private lateinit var repository: FakeAuthRepository
    private lateinit var view: FakeView
    private lateinit var presenter: ForgotPasswordPresenter

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        view = FakeView()
        presenter = ForgotPasswordPresenter(repository)
        presenter.attachView(view)
    }

    @Test
    fun `blank email is rejected`() {
        presenter.onSendResetClicked("   ")

        assertEquals("Email is required", view.emailError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `valid email sends reset and shows sent state`() {
        presenter.onSendResetClicked(" jane.doe@example.com ")

        assertEquals("jane.doe@example.com", repository.lastResetEmail)
        assertEquals("jane.doe@example.com", view.sentTo)
        assertEquals(listOf("clearFieldErrors", "showLoading", "hideLoading", "showResetEmailSent"), view.events)
    }

    @Test
    fun `failure shows message and stays on form`() {
        repository.resetResult = Result.failure(AuthError("No account found for this email, or it has been disabled."))

        presenter.onSendResetClicked("nobody@example.com")

        assertEquals(listOf("No account found for this email, or it has been disabled."), view.messages)
        assertTrue("showResetEmailSent" !in view.events)
    }

    @Test
    fun `back link navigates to login`() {
        presenter.onBackToLoginClicked()
        assertEquals(listOf("navigateToLogin"), view.events)
    }
}
