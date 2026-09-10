package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.EquipmentAdapter
import com.example.eldroid_nullpoint.databinding.ActivitySimpleListBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.DemoData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * "See all" for equipment availability: every SmartDock box, live.
 * The dashboard only previews the first few, this is the full list (FR-10).
 */
class EquipmentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: EquipmentAdapter

    private var listener: ListenerRegistration? = null
    private var currentUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        binding.tvTitle.text = getString(R.string.title_equipment_list)
        binding.tvSubtitle.text = getString(R.string.subtitle_equipment_list)
        binding.tvEmpty.text = getString(R.string.empty_equipment)

        adapter = EquipmentAdapter(currentUid) { equipment ->
            startActivity(EquipmentDetailActivity.intent(this, equipment.id))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.ivBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { restartListening() }
    }

    override fun onStart() {
        super.onStart()
        startListening()
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

        listener = firestore.collection(Equipment.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) render(DemoData.equipment(currentUid), isDemo = true)
                    else showError()
                    return@addSnapshotListener
                }
                val equipment = snapshot?.documents
                    ?.map { Equipment.from(it) }
                    ?.sortedBy { it.boxNumber }
                    .orEmpty()

                if (equipment.isEmpty() && DemoData.ENABLED) {
                    render(DemoData.equipment(currentUid), isDemo = true)
                } else {
                    render(equipment, isDemo = false)
                }
            }
    }

    private fun render(equipment: List<Equipment>, isDemo: Boolean) {
        adapter.submit(equipment)
        showContent(equipment.isEmpty())

        // "3 of 10 available" gives the count at a glance.
        binding.tvSubtitle.text = when {
            equipment.isEmpty() -> getString(R.string.subtitle_equipment_list)
            isDemo -> getString(R.string.demo_subtitle)
            else -> getString(
                R.string.available_count,
                equipment.count { !it.isBorrowed },
                equipment.size
            )
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
