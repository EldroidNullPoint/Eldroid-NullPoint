package com.example.eldroid_nullpoint.data

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.EquipmentTransaction

/**
 * Model-layer access to SmartDock equipment and transaction data.
 * Presenters depend on this interface; [FirestoreEquipmentRepository] is the
 * production implementation.
 */
interface EquipmentRepository {

    /** All monitored boxes, ordered by box number. */
    fun fetchEquipment(callback: RepositoryCallback<List<Equipment>>)

    /** The newest [limit] events belonging to [uid], newest first. */
    fun fetchTransactions(uid: String, limit: Int, callback: RepositoryCallback<List<EquipmentTransaction>>)

    /**
     * Prototype helper: when the equipment collection is empty, writes a set of
     * sample boxes (one of them borrowed by [uid]) so the dashboard has data
     * before the physical tower is connected. Calls back with true when it seeded.
     */
    fun seedSampleDataIfEmpty(uid: String, userName: String, callback: RepositoryCallback<Boolean>)
}
