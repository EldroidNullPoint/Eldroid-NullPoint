package com.example.eldroid_nullpoint.data

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.EquipmentTransaction
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

/**
 * Firestore implementation of [EquipmentRepository].
 *
 * Collections:
 *  - equipment/{id}      one document per monitored box
 *  - transactions/{id}   borrow / return / alert events
 */
class FirestoreEquipmentRepository(
    private val firestore: FirebaseFirestore
) : EquipmentRepository {

    private val equipmentCollection get() = firestore.collection(EQUIPMENT_COLLECTION)
    private val transactionsCollection get() = firestore.collection(TRANSACTIONS_COLLECTION)

    override fun fetchEquipment(callback: RepositoryCallback<List<Equipment>>) {
        equipmentCollection.orderBy("boxNumber", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Equipment::class.java)?.copy(id = doc.id)
                }
                callback(Result.success(items))
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    override fun fetchTransactions(
        uid: String,
        limit: Int,
        callback: RepositoryCallback<List<EquipmentTransaction>>
    ) {
        // Filter only on uid and sort locally so no composite index is required.
        transactionsCollection.whereEqualTo("uid", uid).get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents
                    .mapNotNull { doc -> doc.toObject(EquipmentTransaction::class.java)?.copy(id = doc.id) }
                    .sortedByDescending { it.timestamp }
                    .take(limit)
                callback(Result.success(items))
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    override fun seedSampleDataIfEmpty(
        uid: String,
        userName: String,
        callback: RepositoryCallback<Boolean>
    ) {
        equipmentCollection.limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    callback(Result.success(false))
                    return@addOnSuccessListener
                }
                val now = System.currentTimeMillis()
                val batch = firestore.batch()
                sampleEquipment(uid, userName, now).forEach { item ->
                    batch.set(equipmentCollection.document(item.id), item)
                }
                sampleTransactions(uid, userName, now).forEach { tx ->
                    batch.set(transactionsCollection.document(tx.id), tx)
                }
                batch.commit()
                    .addOnSuccessListener { callback(Result.success(true)) }
                    .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    private fun Throwable.toAuthError(): AuthError {
        val message = when (this) {
            is FirebaseNetworkException -> "No internet connection. Check your network and try again."
            is FirebaseFirestoreException -> when (code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You do not have permission to read equipment data. Check the Firestore rules."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Equipment data is temporarily unavailable. Please try again."
                else -> localizedMessage ?: "Could not load equipment data."
            }
            else -> localizedMessage ?: "Could not load equipment data."
        }
        return AuthError(message, this)
    }

    companion object {
        const val EQUIPMENT_COLLECTION = "equipment"
        const val TRANSACTIONS_COLLECTION = "transactions"

        /** Small shared items the proposal lists as typical SmartDock equipment. */
        internal fun sampleEquipment(uid: String, userName: String, now: Long): List<Equipment> {
            val twoHours = TimeUnit.HOURS.toMillis(2)
            return listOf(
                Equipment(
                    id = "box-01", name = "Wireless Microphone", category = "Audio", boxNumber = 1,
                    status = Equipment.STATUS_BORROWED, borrowedBy = uid, borrowedByName = userName,
                    borrowedAt = now - TimeUnit.MINUTES.toMillis(40), dueAt = now + twoHours
                ),
                Equipment(id = "box-02", name = "HDMI Cable (2 m)", category = "Cables", boxNumber = 2),
                Equipment(id = "box-03", name = "Presentation Remote", category = "Presentation", boxNumber = 3),
                Equipment(
                    id = "box-04", name = "65W USB-C Charger", category = "Power", boxNumber = 4,
                    status = Equipment.STATUS_BORROWED, borrowedBy = "sample-other-borrower",
                    borrowedByName = "Another Borrower",
                    borrowedAt = now - TimeUnit.HOURS.toMillis(5), dueAt = now - TimeUnit.HOURS.toMillis(1)
                ),
                Equipment(id = "box-05", name = "VGA to HDMI Adapter", category = "Adapters", boxNumber = 5),
                Equipment(id = "box-06", name = "Portable Speaker", category = "Audio", boxNumber = 6)
            )
        }

        internal fun sampleTransactions(uid: String, userName: String, now: Long): List<EquipmentTransaction> =
            listOf(
                EquipmentTransaction(
                    id = "tx-sample-1", uid = uid, userName = userName, equipmentId = "box-03",
                    equipmentName = "Presentation Remote", boxNumber = 3,
                    type = EquipmentTransaction.TYPE_BORROW, timestamp = now - TimeUnit.DAYS.toMillis(1)
                ),
                EquipmentTransaction(
                    id = "tx-sample-2", uid = uid, userName = userName, equipmentId = "box-03",
                    equipmentName = "Presentation Remote", boxNumber = 3,
                    type = EquipmentTransaction.TYPE_RETURN,
                    timestamp = now - TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1)
                ),
                EquipmentTransaction(
                    id = "tx-sample-3", uid = uid, userName = userName, equipmentId = "box-01",
                    equipmentName = "Wireless Microphone", boxNumber = 1,
                    type = EquipmentTransaction.TYPE_BORROW, timestamp = now - TimeUnit.MINUTES.toMillis(40)
                )
            )
    }
}
