package com.example.eldroid_nullpoint.splash

import com.example.eldroid_nullpoint.data.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashPresenterTest {

    private class FakeView : SplashContract.View {
        val events = mutableListOf<String>()
        override fun navigateToHome() { events += "navigateToHome" }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    @Test
    fun `signed in user goes home`() {
        val repository = FakeAuthRepository().apply { sessionUser = FakeAuthRepository.EMAIL_USER }
        val view = FakeView()
        SplashPresenter(repository).apply { attachView(view) }.checkSession()

        assertEquals(listOf("navigateToHome"), view.events)
    }

    @Test
    fun `signed out user goes to login`() {
        val repository = FakeAuthRepository()
        val view = FakeView()
        SplashPresenter(repository).apply { attachView(view) }.checkSession()

        assertEquals(listOf("navigateToLogin"), view.events)
    }
}
