package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eldroid_nullpoint.adapter.EquipmentAdapter
import com.example.eldroid_nullpoint.databinding.ActivitySimpleListBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.mvp.equipmentlist.EquipmentListContract
import com.example.eldroid_nullpoint.mvp.equipmentlist.EquipmentListPresenter
import com.google.firebase.auth.FirebaseAuth

class EquipmentListActivity : AppCompatActivity(), EquipmentListContract.View {

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var presenter: EquipmentListContract.Presenter
    private lateinit var adapter: EquipmentAdapter

    private var currentUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        presenter = EquipmentListPresenter(this)
        binding.btnRetry.setOnClickListener { presenter.onRetry() }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart(currentUid)
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
    // EquipmentListContract.View
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

    override fun renderEquipment(equipment: List<Equipment>, isDemo: Boolean) {
        adapter.submit(equipment)
    }

    override fun updateSubtitle(text: String) {
        binding.tvSubtitle.text = text
    }
}
