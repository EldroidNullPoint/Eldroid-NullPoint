package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomePresenterTest {

    private class FakeView : HomeContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var dashboard: DashboardData? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun showDashboard(data: DashboardData) { events += "showDashboard"; dashboard = data }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    private lateinit var repository: FakeAuthRepository
    private lateinit var view: FakeView
    private lateinit var presenter: HomePresenter

    @Before
    fun setUp() {
        repository = FakeAuthRepository().apply { sessionUser = FakeAuthRepository.EMAIL_USER }
        view = FakeView()
        presenter = HomePresenter(
            repository,
            formatDate = { "date:$it" },
            formatDateTime = { "datetime:$it" }
        )
        presenter.attachView(view)
    }

    @Test
    fun `no session redirects to login without fetching`() {
        repository.sessionUser = null
        presenter.loadDashboard()

        assertEquals(listOf("navigateToLogin"), view.events)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `profile is mapped into dashboard data`() {
        presenter.loadDashboard()

        assertEquals("uid-email", repository.lastFetchedUid)
        assertEquals(listOf("showLoading", "hideLoading", "showDashboard"), view.events)

        val data = view.dashboard
        assertNotNull(data)
        data!!
        assertEquals("Jane", data.firstName)
        assertEquals("Jane Doe", data.fullName)
        assertEquals("jane.doe@example.com", data.email)
        assertEquals("JD", data.initials)
        assertEquals("Email & password", data.providerLabel)
        assertEquals("date:1000000", data.memberSince)
        assertEquals("datetime:2000000", data.lastLogin)
        assertEquals(5L, data.loginCount)
        assertTrue(data.canChangePassword)
    }

    @Test
    fun `google account cannot change password`() {
        repository.sessionUser = FakeAuthRepository.GOOGLE_USER
        repository.profileResult = Result.success(
            FakeAuthRepository.PROFILE.copy(firstName = "Gina", lastName = "Google", email = "gina@gmail.com")
        )

        presenter.loadDashboard()

        val data = view.dashboard!!
        assertEquals("Google", data.providerLabel)
        assertFalse(data.canChangePassword)
        assertEquals("GG", data.initials)
    }

    @Test
    fun `profile failure falls back to session data and shows message`() {
        repository.profileResult = Result.failure(AuthError("No internet connection. Check your network and try again."))

        presenter.loadDashboard()

        val data = view.dashboard!!
        assertEquals("Jane", data.firstName)
        assertEquals("Jane Doe", data.fullName)
        assertEquals("jane.doe@example.com", data.email)
        assertEquals("", data.memberSince)
        assertEquals(0L, data.loginCount)
        assertEquals(listOf("No internet connection. Check your network and try again."), view.messages)
    }

    @Test
    fun `initials fall back to email when the name is missing`() {
        repository.profileResult = Result.success(
            FakeAuthRepository.PROFILE.copy(firstName = "", lastName = "", email = "zed@example.com")
        )
        repository.sessionUser = FakeAuthRepository.EMAIL_USER.copy(displayName = "")

        presenter.loadDashboard()

        assertEquals("Z", view.dashboard!!.initials)
    }

    @Test
    fun `logout clears the session and navigates to login`() {
        presenter.onLogoutClicked()

        assertTrue("logout" in repository.calls)
        assertEquals(listOf("navigateToLogin"), view.events)
    }
}
