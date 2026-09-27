package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.TransactionAdapter
import com.example.eldroid_nullpoint.databinding.ActivitySimpleListBinding
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.mvp.activityhistory.ActivityHistoryContract
import com.example.eldroid_nullpoint.mvp.activityhistory.ActivityHistoryPresenter
import com.google.firebase.auth.FirebaseAuth

class ActivityHistoryActivity : AppCompatActivity(), ActivityHistoryContract.View {

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var presenter: ActivityHistoryContract.Presenter
    private lateinit var adapter: TransactionAdapter

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

        binding.tvTitle.text = getString(R.string.title_activity_history)
        binding.tvSubtitle.text = getString(R.string.subtitle_activity_history)
        binding.tvEmpty.text = getString(R.string.empty_activity)

        adapter = TransactionAdapter { transaction ->
            if (transaction.equipmentId.isNotBlank()) {
                startActivity(EquipmentDetailActivity.intent(this, transaction.equipmentId))
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.ivBack.setOnClickListener { finish() }

        presenter = ActivityHistoryPresenter(this)
        binding.btnRetry.setOnClickListener { presenter.onRetry() }
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
    // ActivityHistoryContract.View
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
        binding.errorState.visibility = View.VISIBLE
    }

    override fun renderTransactions(transactions: List<Transaction>, isDemo: Boolean) {
        adapter.submit(transactions)
        binding.tvSubtitle.text = if (isDemo) {
            getString(R.string.demo_subtitle)
        } else {
            getString(R.string.subtitle_activity_history)
        }
    }
}
