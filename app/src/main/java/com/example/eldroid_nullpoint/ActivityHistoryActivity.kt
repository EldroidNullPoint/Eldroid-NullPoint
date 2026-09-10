package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.TransactionAdapter
import com.example.eldroid_nullpoint.databinding.ActivitySimpleListBinding
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.DemoData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * "See all" for recent activity: the borrower's own complete borrow/return
 * history (FR-10), newest first. Scoped to the signed-in borrower only -
 * system-wide history belongs to the administrator web dashboard.
 */
class ActivityHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: TransactionAdapter

    private var listener: ListenerRegistration? = null
    private var currentUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
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
        binding.btnRetry.setOnClickListener { restartListening() }
    }

    override fun onStart() {
        super.onStart()
        if (currentUid.isNotBlank()) startListening()
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    private fun restartListening() {
        stopListening()
        startListening()
    }

    private fun startListening() {
        if (listener != null) return
        showLoading()

        // Equality-only filter, sorted client-side - see the note in HomeActivity
        // about why this avoids needing a composite index.
        listener = firestore.collection(Transaction.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) render(DemoData.transactions(currentUid, ""), true)
                    else showError()
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents
                    ?.map { Transaction.from(it) }
                    ?.filter { it.uid.isNotBlank() }
                    ?.sortedByDescending { it.timestamp }
                    .orEmpty()

                if (transactions.isEmpty() && DemoData.ENABLED) {
                    render(DemoData.transactions(currentUid, ""), isDemo = true)
                } else {
                    render(transactions, isDemo = false)
                }
            }
    }

    private fun render(transactions: List<Transaction>, isDemo: Boolean) {
        adapter.submit(transactions)
        showContent(transactions.isEmpty())
        binding.tvSubtitle.text = if (isDemo) {
            getString(R.string.demo_subtitle)
        } else {
            getString(R.string.subtitle_activity_history)
        }
    }

    private fun stopListening() {
        listener?.remove()
        listener = null
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
    }

    private fun showContent(isEmpty: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showError() {
        stopListening()
        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
    }
}
