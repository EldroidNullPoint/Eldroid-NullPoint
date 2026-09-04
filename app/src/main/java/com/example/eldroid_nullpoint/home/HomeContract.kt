package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.base.BaseView

interface HomeContract {

    interface View : BaseView {
        fun showDashboard(data: DashboardData)
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        /** Loads the signed-in user's profile and pushes a [DashboardData] to the view. */
        fun loadDashboard()
        fun onLogoutClicked()
    }
}

/**
 * Everything the Home/Dashboard screen displays, already formatted so the View
 * just puts strings into TextViews.
 */
data class DashboardData(
    val firstName: String,
    val fullName: String,
    val email: String,
    /** Up to two uppercase letters for an avatar, e.g. "JD". */
    val initials: String,
    /** "Email & password" or "Google". */
    val providerLabel: String,
    /** Formatted account-creation date, e.g. "Sep 4, 2026". */
    val memberSince: String,
    /** Formatted last-login date/time, or "" when unknown. */
    val lastLogin: String,
    val loginCount: Long,
    val canChangePassword: Boolean
)
