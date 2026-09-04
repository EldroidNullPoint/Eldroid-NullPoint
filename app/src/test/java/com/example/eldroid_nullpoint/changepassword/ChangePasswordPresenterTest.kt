package com.example.eldroid_nullpoint.changepassword

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChangePasswordPresenterTest {

    private class FakeView : ChangePasswordContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var currentPasswordError: String? = null
        var newPasswordError: String? = null
        var confirmPasswordError: String? = null
        var unavailableMessage: String? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun clearFieldErrors() {
            events += "clearFieldErrors"
            currentPasswordError = null; newPasswordError = null; confirmPasswordError = null
        }
        override fun showCurrentPasswordError(message: String) { currentPasswordError = message }
        override fun showNewPasswordError(message: String) { newPasswordError = message }
        override fun showConfirmPasswordError(message: String) { confirmPasswordError = message }
        override fun showPasswordChangeUnavailable(message: String) {
            events += "showPasswordChangeUnavailable"; unavailableMessage = message
        }
        override fun showPasswordChanged() { events += "showPasswordChanged" }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    private val current = "Tr0ub4dor&3x"
    private val strongNew = "Correct-Horse9!"

    private lateinit var repository: FakeAuthRepository
    private lateinit var view: FakeView
    private lateinit var presenter: ChangePasswordPresenter

    @Before
    fun setUp() {
        repository = FakeAuthRepository().apply { sessionUser = FakeAuthRepository.EMAIL_USER }
        view = FakeView()
        presenter = ChangePasswordPresenter(repository)
        presenter.attachView(view)
    }

    @Test
    fun `start with no session goes to login`() {
        repository.sessionUser = null
        presenter.start()
        assertEquals(listOf("navigateToLogin"), view.events)
    }

    @Test
    fun `start with google account disables the form`() {
        repository.sessionUser = FakeAuthRepository.GOOGLE_USER
        presenter.start()
        assertEquals(listOf("showPasswordChangeUnavailable"), view.events)
        assertEquals(ChangePasswordPresenter.UNAVAILABLE_MESSAGE, view.unavailableMessage)
    }

    @Test
    fun `start with email account shows nothing special`() {
        presenter.start()
        assertTrue(view.events.isEmpty())
    }

    @Test
    fun `empty current password and weak new password are both reported`() {
        presenter.onChangePasswordClicked("", "short", "short")

        assertEquals("Current password is required", view.currentPasswordError)
        assertEquals("Password must be at least 10 characters", view.newPasswordError)
        assertNull(view.confirmPasswordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `new password equal to current is rejected`() {
        presenter.onChangePasswordClicked(current, current, current)

        assertEquals("New password must be different from your current password", view.newPasswordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `new password containing the user's name is rejected`() {
        presenter.onChangePasswordClicked(current, "Janexyq!19Q", "Janexyq!19Q")
        assertEquals("Password must not contain your name or email", view.newPasswordError)
    }

    @Test
    fun `confirmation mismatch is rejected`() {
        presenter.onChangePasswordClicked(current, strongNew, strongNew + "z")
        assertEquals("Passwords do not match", view.confirmPasswordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `valid input changes password and reports success`() {
        presenter.onChangePasswordClicked(current, strongNew, strongNew)

        assertEquals(current to strongNew, repository.lastChangePasswordArgs)
        assertEquals(listOf("clearFieldErrors", "showLoading", "hideLoading", "showPasswordChanged"), view.events)
    }

    @Test
    fun `wrong current password surfaces repository message`() {
        repository.changePasswordResult = Result.failure(AuthError("Current password is incorrect."))

        presenter.onChangePasswordClicked("Wr0ng-Pass!x", strongNew, strongNew)

        assertEquals(listOf("Current password is incorrect."), view.messages)
        assertTrue("showPasswordChanged" !in view.events)
    }
}
