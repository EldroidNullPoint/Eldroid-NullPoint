package com.example.eldroid_nullpoint.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.changepassword.ChangePasswordActivity
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivityHomeBinding
import com.example.eldroid_nullpoint.login.LoginActivity

/**
 * SmartDock borrower dashboard. Renders the [DashboardData] the presenter provides:
 * greeting, availability stats, the user's borrowed items with due times, the
 * equipment list, recent activity and the account card.
 */
class HomeActivity : AppCompatActivity(), HomeContract.View {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var presenter: HomeContract.Presenter

    private val myItemsAdapter = EquipmentAdapter()
    private val equipmentAdapter = EquipmentAdapter()
    private val activityAdapter = ActivityAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = HomePresenter(
            Injection.provideAuthRepository(this),
            Injection.provideEquipmentRepository()
        )
        presenter.attachView(this)

        setupLists()
        binding.btnChangePassword.setOnClickListener { presenter.onChangePasswordClicked() }
        binding.btnLogout.setOnClickListener { presenter.onLogoutClicked() }
    }

    /** Reloading on every start keeps the dashboard fresh after Change Password or a tower event. */
    override fun onStart() {
        super.onStart()
        presenter.loadDashboard()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    private fun setupLists() {
        binding.rvMyItems.layoutManager = LinearLayoutManager(this)
        binding.rvMyItems.adapter = myItemsAdapter
        binding.rvEquipment.layoutManager = LinearLayoutManager(this)
        binding.rvEquipment.adapter = equipmentAdapter
        binding.rvActivity.layoutManager = LinearLayoutManager(this)
        binding.rvActivity.adapter = activityAdapter
    }

    // ---------------------------------------------------------------
    // HomeContract.View
    // ---------------------------------------------------------------

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
    }

    override fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showDashboard(data: DashboardData) {
        // Header
        binding.tvAvatar.text = data.initials
        binding.tvGreeting.text = if (data.firstName.isNotBlank()) {
            getString(R.string.home_greeting, data.firstName)
        } else {
            getString(R.string.home_greeting_generic)
        }
        binding.tvEmail.text = data.email

        // Overdue banner
        if (data.stats.myOverdue > 0) {
            binding.tvOverdueBanner.text = getString(R.string.overdue_banner, data.stats.myOverdue)
            binding.tvOverdueBanner.visibility = View.VISIBLE
        } else {
            binding.tvOverdueBanner.visibility = View.GONE
        }

        // Stats
        binding.tvStatAvailable.text = data.stats.available.toString()
        binding.tvStatBorrowed.text = data.stats.borrowed.toString()
        binding.tvStatMine.text = data.stats.myActive.toString()

        // Lists
        myItemsAdapter.submitList(data.myBorrowedItems)
        toggleList(binding.rvMyItems, binding.tvMyItemsEmpty, data.myBorrowedItems.isNotEmpty())

        equipmentAdapter.submitList(data.equipment)
        toggleList(binding.rvEquipment, binding.tvEquipmentEmpty, data.equipment.isNotEmpty())

        activityAdapter.submitList(data.recentActivity)
        toggleList(binding.rvActivity, binding.tvActivityEmpty, data.recentActivity.isNotEmpty())

        // Account card
        val unknown = getString(R.string.value_unknown)
        binding.tvProvider.text = data.providerLabel
        binding.tvMemberSince.text = data.memberSince.ifBlank { unknown }
        binding.tvLastLogin.text = data.lastLogin.ifBlank { unknown }
        binding.tvLoginCount.text = data.loginCount.toString()
        binding.btnChangePassword.visibility = if (data.canChangePassword) View.VISIBLE else View.GONE
    }

    override fun navigateToChangePassword() {
        startActivity(Intent(this, ChangePasswordActivity::class.java))
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toggleList(list: View, empty: View, hasItems: Boolean) {
        list.visibility = if (hasItems) View.VISIBLE else View.GONE
        empty.visibility = if (hasItems) View.GONE else View.VISIBLE
    }
}
