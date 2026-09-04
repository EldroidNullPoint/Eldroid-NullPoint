package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.data.SessionUser
import com.example.eldroid_nullpoint.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomePresenter(
    private val repository: AuthRepository,
    /** Injectable so tests are not locale/timezone dependent. */
    private val formatDate: (Long) -> String = ::defaultDateFormat,
    private val formatDateTime: (Long) -> String = ::defaultDateTimeFormat
) : BasePresenter<HomeContract.View>(), HomeContract.Presenter {

    override fun loadDashboard() {
        val session = repository.currentUser()
        if (session == null) {
            view?.navigateToLogin()
            return
        }

        view?.showLoading()
        repository.fetchProfile(session.uid) { result ->
            view?.hideLoading()
            result
                .onSuccess { profile -> view?.showDashboard(buildDashboard(profile, session)) }
                .onFailure {
                    // Auth still knows who the user is, so show what we can.
                    view?.showDashboard(buildDashboard(fallbackProfile(session), session))
                    view?.showMessage(it.message ?: "Could not load your profile.")
                }
        }
    }

    override fun onLogoutClicked() {
        repository.logout()
        view?.navigateToLogin()
    }

    // ---------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------

    private fun buildDashboard(profile: User, session: SessionUser): DashboardData {
        val firstName = profile.firstName.ifBlank { session.displayName.substringBefore(' ') }
        val fullName = profile.fullName.ifBlank { session.displayName }
        val email = profile.email.ifBlank { session.email }

        return DashboardData(
            firstName = firstName,
            fullName = fullName,
            email = email,
            initials = initialsFor(firstName, profile.lastName, email),
            providerLabel = if (session.hasPasswordProvider) "Email & password" else "Google",
            memberSince = if (profile.createdAt > 0) formatDate(profile.createdAt) else "",
            lastLogin = if (profile.lastLoginAt > 0) formatDateTime(profile.lastLoginAt) else "",
            loginCount = profile.loginCount,
            canChangePassword = session.hasPasswordProvider
        )
    }

    private fun fallbackProfile(session: SessionUser): User {
        val parts = session.displayName.trim().split(" ", limit = 2)
        return User(
            uid = session.uid,
            firstName = parts.getOrNull(0).orEmpty(),
            lastName = parts.getOrNull(1).orEmpty(),
            email = session.email,
            provider = session.providerId,
            createdAt = 0L
        )
    }

    private fun initialsFor(firstName: String, lastName: String, email: String): String {
        val fromName = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull() }
            .joinToString("")
        val initials = fromName.ifEmpty { email.trim().take(1) }
        return initials.uppercase().ifEmpty { "?" }
    }
}

internal fun defaultDateFormat(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

internal fun defaultDateTimeFormat(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(millis))
