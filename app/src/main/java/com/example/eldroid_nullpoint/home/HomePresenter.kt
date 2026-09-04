package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.data.EquipmentRepository
import com.example.eldroid_nullpoint.data.SessionUser
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.EquipmentTransaction
import com.example.eldroid_nullpoint.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomePresenter(
    private val authRepository: AuthRepository,
    private val equipmentRepository: EquipmentRepository,
    /** Injectable so tests are not locale / timezone / clock dependent. */
    private val formatDate: (Long) -> String = ::defaultDateFormat,
    private val formatDateTime: (Long) -> String = ::defaultDateTimeFormat,
    private val now: () -> Long = System::currentTimeMillis
) : BasePresenter<HomeContract.View>(), HomeContract.Presenter {

    override fun loadDashboard() {
        val session = authRepository.currentUser()
        if (session == null) {
            view?.navigateToLogin()
            return
        }

        view?.showLoading()
        authRepository.fetchProfile(session.uid) { profileResult ->
            val profile = profileResult.getOrElse { error ->
                // Auth still knows who the user is, so show what we can.
                view?.showMessage(error.message ?: "Could not load your profile.")
                fallbackProfile(session)
            }
            loadEquipment(session, profile)
        }
    }

    override fun onChangePasswordClicked() {
        view?.navigateToChangePassword()
    }

    override fun onLogoutClicked() {
        authRepository.logout()
        view?.navigateToLogin()
    }

    // ---------------------------------------------------------------
    // Loading chain: profile -> (seed) -> equipment -> transactions
    // ---------------------------------------------------------------

    private fun loadEquipment(session: SessionUser, profile: User) {
        val userName = profile.fullName.ifBlank { session.displayName }
        equipmentRepository.seedSampleDataIfEmpty(session.uid, userName) {
            // Seeding is best-effort; whether it ran or failed we still read what exists.
            equipmentRepository.fetchEquipment { equipmentResult ->
                val equipment = equipmentResult.getOrElse { error ->
                    view?.showMessage(error.message ?: "Could not load equipment.")
                    emptyList()
                }
                loadTransactions(session, profile, equipment)
            }
        }
    }

    private fun loadTransactions(session: SessionUser, profile: User, equipment: List<Equipment>) {
        equipmentRepository.fetchTransactions(session.uid, RECENT_ACTIVITY_LIMIT) { txResult ->
            val transactions = txResult.getOrElse { emptyList() }
            view?.hideLoading()
            view?.showDashboard(buildDashboard(profile, session, equipment, transactions))
        }
    }

    // ---------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------

    private fun buildDashboard(
        profile: User,
        session: SessionUser,
        equipment: List<Equipment>,
        transactions: List<EquipmentTransaction>
    ): DashboardData {
        val firstName = profile.firstName.ifBlank { session.displayName.substringBefore(' ') }
        val fullName = profile.fullName.ifBlank { session.displayName }
        val email = profile.email.ifBlank { session.email }
        val currentTime = now()

        val mine = equipment.filter { it.isBorrowedBy(session.uid) }
            .sortedBy { it.dueAt }

        return DashboardData(
            firstName = firstName,
            fullName = fullName,
            email = email,
            initials = initialsFor(firstName, profile.lastName, email),
            providerLabel = if (session.hasPasswordProvider) "Email & password" else "Google",
            memberSince = if (profile.createdAt > 0) formatDate(profile.createdAt) else "",
            lastLogin = if (profile.lastLoginAt > 0) formatDateTime(profile.lastLoginAt) else "",
            loginCount = profile.loginCount,
            canChangePassword = session.hasPasswordProvider,
            stats = DashboardStats(
                totalBoxes = equipment.size,
                available = equipment.count { it.isAvailable },
                borrowed = equipment.count { !it.isAvailable },
                myActive = mine.size,
                myOverdue = mine.count { it.isOverdue(currentTime) }
            ),
            myBorrowedItems = mine.map { it.toBorrowedRow(currentTime) },
            equipment = equipment.sortedBy { it.boxNumber }.map { it.toAvailabilityRow(session.uid, currentTime) },
            recentActivity = transactions.map { it.toActivityRow() }
        )
    }

    private fun Equipment.toBorrowedRow(currentTime: Long): EquipmentRow {
        val overdue = isOverdue(currentTime)
        return EquipmentRow(
            id = id,
            boxLabel = boxLabel(boxNumber),
            name = name,
            detail = if (borrowedAt > 0) "Borrowed ${formatDateTime(borrowedAt)}" else "Box $boxNumber",
            badge = dueText(dueAt, currentTime),
            status = if (overdue) EquipmentStatus.OVERDUE else EquipmentStatus.BORROWED
        )
    }

    private fun Equipment.toAvailabilityRow(uid: String, currentTime: Long): EquipmentRow {
        val detail = listOf("Box $boxNumber", category).filter { it.isNotBlank() }.joinToString(" · ")
        return when {
            isAvailable -> EquipmentRow(id, boxLabel(boxNumber), name, detail, "Available", EquipmentStatus.AVAILABLE)
            isBorrowedBy(uid) -> EquipmentRow(
                id, boxLabel(boxNumber), name, "$detail · Borrowed by you",
                if (isOverdue(currentTime)) "Overdue" else "Yours",
                if (isOverdue(currentTime)) EquipmentStatus.OVERDUE else EquipmentStatus.BORROWED
            )
            else -> EquipmentRow(
                id, boxLabel(boxNumber), name, detail,
                if (isOverdue(currentTime)) "Overdue" else "Borrowed",
                if (isOverdue(currentTime)) EquipmentStatus.OVERDUE else EquipmentStatus.BORROWED
            )
        }
    }

    private fun EquipmentTransaction.toActivityRow(): ActivityRow {
        val title = when (type) {
            EquipmentTransaction.TYPE_BORROW -> "Borrowed $equipmentName"
            EquipmentTransaction.TYPE_RETURN -> "Returned $equipmentName"
            EquipmentTransaction.TYPE_OVERDUE -> "$equipmentName is overdue"
            EquipmentTransaction.TYPE_ALERT -> "Alert on $equipmentName"
            else -> equipmentName
        }
        val time = if (timestamp > 0) formatDateTime(timestamp) else ""
        val subtitle = listOf("Box $boxNumber", time).filter { it.isNotBlank() }.joinToString(" · ")
        return ActivityRow(title = title, subtitle = subtitle, type = type)
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

    companion object {
        const val RECENT_ACTIVITY_LIMIT = 10

        internal fun boxLabel(boxNumber: Int): String = boxNumber.toString().padStart(2, '0')

        /** "Due in 1h 20m", "Overdue by 45m", or "No due time" when unknown. */
        internal fun dueText(dueAt: Long, now: Long): String {
            if (dueAt <= 0) return "No due time"
            val diff = dueAt - now
            return if (diff >= 0) "Due in ${durationText(diff)}" else "Overdue by ${durationText(-diff)}"
        }

        internal fun durationText(millis: Long): String {
            val days = TimeUnit.MILLISECONDS.toDays(millis)
            val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            return when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "less than a minute"
            }
        }
    }
}

internal fun defaultDateFormat(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

internal fun defaultDateTimeFormat(millis: Long): String =
    SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(Date(millis))
