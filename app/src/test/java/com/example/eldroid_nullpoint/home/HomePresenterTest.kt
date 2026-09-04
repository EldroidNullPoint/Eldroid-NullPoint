package com.example.eldroid_nullpoint.home

import com.example.eldroid_nullpoint.data.AuthError
import com.example.eldroid_nullpoint.data.FakeAuthRepository
import com.example.eldroid_nullpoint.data.FakeEquipmentRepository
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.EquipmentTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HomePresenterTest {

    private class FakeView : HomeContract.View {
        val events = mutableListOf<String>()
        val messages = mutableListOf<String>()
        var dashboard: DashboardData? = null

        override fun showLoading() { events += "showLoading" }
        override fun hideLoading() { events += "hideLoading" }
        override fun showMessage(message: String) { events += "showMessage"; messages += message }
        override fun showDashboard(data: DashboardData) { events += "showDashboard"; dashboard = data }
        override fun navigateToChangePassword() { events += "navigateToChangePassword" }
        override fun navigateToLogin() { events += "navigateToLogin" }
    }

    private val now = 10_000_000_000L
    private val uid = FakeAuthRepository.EMAIL_USER.uid

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var equipmentRepository: FakeEquipmentRepository
    private lateinit var view: FakeView
    private lateinit var presenter: HomePresenter

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository().apply { sessionUser = FakeAuthRepository.EMAIL_USER }
        equipmentRepository = FakeEquipmentRepository()
        view = FakeView()
        presenter = HomePresenter(
            authRepository,
            equipmentRepository,
            formatDate = { "date:$it" },
            formatDateTime = { "datetime:$it" },
            now = { now }
        )
        presenter.attachView(view)
    }

    private fun sampleEquipment() = listOf(
        Equipment(id = "box-02", name = "HDMI Cable", category = "Cables", boxNumber = 2),
        Equipment(
            id = "box-01", name = "Wireless Microphone", category = "Audio", boxNumber = 1,
            status = Equipment.STATUS_BORROWED, borrowedBy = uid, borrowedByName = "Jane Doe",
            borrowedAt = now - TimeUnit.MINUTES.toMillis(40), dueAt = now + TimeUnit.MINUTES.toMillis(80)
        ),
        Equipment(
            id = "box-03", name = "Presentation Remote", category = "Presentation", boxNumber = 3,
            status = Equipment.STATUS_BORROWED, borrowedBy = uid,
            borrowedAt = now - TimeUnit.HOURS.toMillis(3), dueAt = now - TimeUnit.MINUTES.toMillis(45)
        ),
        Equipment(
            id = "box-04", name = "Charger", category = "Power", boxNumber = 4,
            status = Equipment.STATUS_BORROWED, borrowedBy = "someone-else",
            borrowedAt = now - TimeUnit.HOURS.toMillis(1), dueAt = now + TimeUnit.HOURS.toMillis(1)
        )
    )

    @Test
    fun `no session redirects to login without fetching`() {
        authRepository.sessionUser = null
        presenter.loadDashboard()

        assertEquals(listOf("navigateToLogin"), view.events)
        assertTrue(authRepository.calls.isEmpty())
        assertTrue(equipmentRepository.calls.isEmpty())
    }

    @Test
    fun `loads profile, seeds, then equipment and transactions in order`() {
        presenter.loadDashboard()

        assertEquals(listOf("fetchProfile"), authRepository.calls)
        assertEquals(
            listOf("seedSampleDataIfEmpty", "fetchEquipment", "fetchTransactions"),
            equipmentRepository.calls
        )
        assertEquals(uid to "Jane Doe", equipmentRepository.lastSeedArgs)
        assertEquals(uid, equipmentRepository.lastTransactionsUid)
        assertEquals(HomePresenter.RECENT_ACTIVITY_LIMIT, equipmentRepository.lastTransactionsLimit)
        assertEquals(listOf("showLoading", "hideLoading", "showDashboard"), view.events)
    }

    @Test
    fun `account section is mapped from the profile`() {
        presenter.loadDashboard()

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
    fun `equipment is mapped into stats, my items and availability rows`() {
        equipmentRepository.equipmentResult = Result.success(sampleEquipment())

        presenter.loadDashboard()
        val data = view.dashboard!!

        assertEquals(DashboardStats(totalBoxes = 4, available = 1, borrowed = 3, myActive = 2, myOverdue = 1), data.stats)

        // My items are sorted by due time (overdue first), with a due/overdue badge.
        assertEquals(listOf("Presentation Remote", "Wireless Microphone"), data.myBorrowedItems.map { it.name })
        val overdue = data.myBorrowedItems[0]
        assertEquals("03", overdue.boxLabel)
        assertEquals("Overdue by 45m", overdue.badge)
        assertEquals(EquipmentStatus.OVERDUE, overdue.status)
        val mic = data.myBorrowedItems[1]
        assertEquals("Due in 1h 20m", mic.badge)
        assertEquals(EquipmentStatus.BORROWED, mic.status)
        assertTrue(mic.detail.startsWith("Borrowed datetime:"))

        // Availability list is ordered by box number and labels who has what.
        assertEquals(listOf("01", "02", "03", "04"), data.equipment.map { it.boxLabel })
        assertEquals("Yours", data.equipment[0].badge)
        assertEquals("Available", data.equipment[1].badge)
        assertEquals(EquipmentStatus.AVAILABLE, data.equipment[1].status)
        assertEquals("Box 2 · Cables", data.equipment[1].detail)
        assertEquals("Overdue", data.equipment[2].badge)
        assertEquals("Borrowed", data.equipment[3].badge)
        assertEquals(EquipmentStatus.BORROWED, data.equipment[3].status)
    }

    @Test
    fun `transactions become activity rows`() {
        equipmentRepository.transactionsResult = Result.success(
            listOf(
                EquipmentTransaction(
                    uid = uid, equipmentName = "Wireless Microphone", boxNumber = 1,
                    type = EquipmentTransaction.TYPE_BORROW, timestamp = 5_000L
                ),
                EquipmentTransaction(
                    uid = uid, equipmentName = "HDMI Cable", boxNumber = 2,
                    type = EquipmentTransaction.TYPE_RETURN, timestamp = 4_000L
                ),
                EquipmentTransaction(
                    uid = uid, equipmentName = "Charger", boxNumber = 4,
                    type = EquipmentTransaction.TYPE_OVERDUE, timestamp = 3_000L
                )
            )
        )

        presenter.loadDashboard()
        val rows = view.dashboard!!.recentActivity

        assertEquals(3, rows.size)
        assertEquals("Borrowed Wireless Microphone", rows[0].title)
        assertEquals("Box 1 · datetime:5000", rows[0].subtitle)
        assertEquals("Returned HDMI Cable", rows[1].title)
        assertEquals("Charger is overdue", rows[2].title)
        assertEquals(EquipmentTransaction.TYPE_OVERDUE, rows[2].type)
    }

    @Test
    fun `google account cannot change password`() {
        authRepository.sessionUser = FakeAuthRepository.GOOGLE_USER
        authRepository.profileResult = Result.success(
            FakeAuthRepository.PROFILE.copy(firstName = "Gina", lastName = "Google", email = "gina@gmail.com")
        )

        presenter.loadDashboard()

        val data = view.dashboard!!
        assertEquals("Google", data.providerLabel)
        assertFalse(data.canChangePassword)
        assertEquals("GG", data.initials)
    }

    @Test
    fun `profile failure falls back to session data, still loads equipment, and shows message`() {
        authRepository.profileResult = Result.failure(AuthError("No internet connection. Check your network and try again."))
        equipmentRepository.equipmentResult = Result.success(sampleEquipment())

        presenter.loadDashboard()

        val data = view.dashboard!!
        assertEquals("Jane", data.firstName)
        assertEquals("Jane Doe", data.fullName)
        assertEquals("", data.memberSince)
        assertEquals(4, data.stats.totalBoxes)
        assertEquals(listOf("No internet connection. Check your network and try again."), view.messages)
        assertTrue("showDashboard" in view.events)
    }

    @Test
    fun `equipment failure shows message and an empty dashboard`() {
        equipmentRepository.equipmentResult = Result.failure(AuthError("You do not have permission to read equipment data. Check the Firestore rules."))

        presenter.loadDashboard()

        val data = view.dashboard!!
        assertEquals(0, data.stats.totalBoxes)
        assertTrue(data.equipment.isEmpty())
        assertEquals(1, view.messages.size)
        assertTrue("hideLoading" in view.events)
    }

    @Test
    fun `initials fall back to email when the name is missing`() {
        authRepository.profileResult = Result.success(
            FakeAuthRepository.PROFILE.copy(firstName = "", lastName = "", email = "zed@example.com")
        )
        authRepository.sessionUser = FakeAuthRepository.EMAIL_USER.copy(displayName = "")

        presenter.loadDashboard()

        assertEquals("Z", view.dashboard!!.initials)
    }

    @Test
    fun `change password navigates`() {
        presenter.onChangePasswordClicked()
        assertEquals(listOf("navigateToChangePassword"), view.events)
    }

    @Test
    fun `logout clears the session and navigates to login`() {
        presenter.onLogoutClicked()

        assertTrue("logout" in authRepository.calls)
        assertEquals(listOf("navigateToLogin"), view.events)
    }

    @Test
    fun `due text helper`() {
        assertEquals("No due time", HomePresenter.dueText(0, now))
        assertEquals("Due in 2d 3h", HomePresenter.dueText(now + TimeUnit.HOURS.toMillis(51), now))
        assertEquals("Due in 1h 20m", HomePresenter.dueText(now + TimeUnit.MINUTES.toMillis(80), now))
        assertEquals("Due in 5m", HomePresenter.dueText(now + TimeUnit.MINUTES.toMillis(5), now))
        assertEquals("Due in less than a minute", HomePresenter.dueText(now + 10_000, now))
        assertEquals("Overdue by 45m", HomePresenter.dueText(now - TimeUnit.MINUTES.toMillis(45), now))
        assertEquals("07", HomePresenter.boxLabel(7))
        assertEquals("12", HomePresenter.boxLabel(12))
    }
}
