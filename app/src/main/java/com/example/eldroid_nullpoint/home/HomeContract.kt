package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.base.BaseView

interface HomeContract {

    interface View : BaseView {
        fun showDashboard(data: DashboardData)
        fun navigateToChangePassword()
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        /** Loads profile + equipment + activity and pushes a [DashboardData] to the view. */
        fun loadDashboard()
        fun onChangePasswordClicked()
        fun onLogoutClicked()
    }
}

/**
 * Everything the SmartDock borrower dashboard displays, already formatted so
 * the View only has to put strings into widgets.
 */
data class DashboardData(
    // Account
    val firstName: String,
    val fullName: String,
    val email: String,
    /** Up to two uppercase letters for the avatar, e.g. "JD". */
    val initials: String,
    /** "Email & password" or "Google". */
    val providerLabel: String,
    val memberSince: String,
    val lastLogin: String,
    val loginCount: Long,
    val canChangePassword: Boolean,
    // SmartDock
    val stats: DashboardStats,
    val myBorrowedItems: List<EquipmentRow>,
    val equipment: List<EquipmentRow>,
    val recentActivity: List<ActivityRow>
)

data class DashboardStats(
    val totalBoxes: Int,
    val available: Int,
    val borrowed: Int,
    val myActive: Int,
    val myOverdue: Int
)

enum class EquipmentStatus { AVAILABLE, BORROWED, OVERDUE }

/** One row in the "My borrowed items" or "Equipment availability" lists. */
data class EquipmentRow(
    val id: String,
    /** Two-digit box number, e.g. "01". */
    val boxLabel: String,
    val name: String,
    /** Secondary line, e.g. "Box 1 · Audio" or "Borrowed Sep 4 at 2:10 PM". */
    val detail: String,
    /** Chip text, e.g. "Available", "Borrowed", "Due in 1h 20m", "Overdue by 45m". */
    val badge: String,
    val status: EquipmentStatus
)

/** One row in the "Recent activity" list. */
data class ActivityRow(
    val title: String,
    val subtitle: String,
    val type: String
)
