package com.example.eldroid_nullpoint.data

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.EquipmentTransaction

/** In-memory [EquipmentRepository] for presenter tests. Responds synchronously. */
class FakeEquipmentRepository : EquipmentRepository {

    var equipmentResult: Result<List<Equipment>> = Result.success(emptyList())
    var transactionsResult: Result<List<EquipmentTransaction>> = Result.success(emptyList())
    var seedResult: Result<Boolean> = Result.success(false)

    val calls = mutableListOf<String>()
    var lastTransactionsUid: String? = null
    var lastTransactionsLimit: Int? = null
    var lastSeedArgs: Pair<String, String>? = null

    override fun fetchEquipment(callback: RepositoryCallback<List<Equipment>>) {
        calls += "fetchEquipment"
        callback(equipmentResult)
    }

    override fun fetchTransactions(
        uid: String,
        limit: Int,
        callback: RepositoryCallback<List<EquipmentTransaction>>
    ) {
        calls += "fetchTransactions"
        lastTransactionsUid = uid
        lastTransactionsLimit = limit
        callback(transactionsResult)
    }

    override fun seedSampleDataIfEmpty(
        uid: String,
        userName: String,
        callback: RepositoryCallback<Boolean>
    ) {
        calls += "seedSampleDataIfEmpty"
        lastSeedArgs = uid to userName
        callback(seedResult)
    }
}
