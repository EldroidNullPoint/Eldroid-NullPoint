package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.NotificationAdapter
import com.example.eldroid_nullpoint.databinding.ActivitySimpleListBinding
import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.mvp.notifications.NotificationsContract
import com.example.eldroid_nullpoint.mvp.notifications.NotificationsPresenter
import com.google.firebase.auth.FirebaseAuth

class NotificationsActivity : AppCompatActivity(), NotificationsContract.View {

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var presenter: NotificationsContract.Presenter
    private lateinit var adapter: NotificationAdapter

    private var currentUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (currentUid.isBlank()) {
            finish()
            return
        }

        binding.tvTitle.text = getString(R.string.title_notifications)
        binding.tvSubtitle.text = getString(R.string.subtitle_notifications)
        binding.tvEmpty.text = getString(R.string.empty_notifications)
        binding.btnHeaderAction.text = getString(R.string.mark_all_read)

        adapter = NotificationAdapter { notification -> presenter.onNotificationClicked(notification) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.ivBack.setOnClickListener { finish() }

        presenter = NotificationsPresenter(
            view = this,
            onOpenEquipment = { equipmentId ->
                startActivity(EquipmentDetailActivity.intent(this, equipmentId))
            }
        )

        binding.btnRetry.setOnClickListener { presenter.onRetry() }
        binding.btnHeaderAction.setOnClickListener { presenter.onMarkAllReadClicked() }
    }

    override fun onStart() {
        super.onStart()
        if (currentUid.isNotBlank()) presenter.onStart(currentUid)
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    // ---------------------------------------------------------------
    // NotificationsContract.View
    // ---------------------------------------------------------------

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
    }

    override fun showContent(isEmpty: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun showError() {
        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.btnHeaderAction.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
    }

    override fun renderNotifications(notifications: List<AppNotification>) {
        adapter.submit(notifications)
    }

    override fun showMarkAllReadButton(visible: Boolean) {
        binding.btnHeaderAction.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
