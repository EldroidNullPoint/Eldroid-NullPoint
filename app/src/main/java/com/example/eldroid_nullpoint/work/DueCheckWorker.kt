package com.example.eldroid_nullpoint.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.LoanPolicy
import com.example.eldroid_nullpoint.util.NotificationPrefs
import com.example.eldroid_nullpoint.util.Notifier
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * FR-06: checks the signed-in borrower's active loans and raises a "due soon"
 * reminder inside the last hour and an "overdue" alert once the due time passes.
 *
 * Each reminder is stored in `notifications` under a deterministic id
 * ([AppNotification.docId]) before it is shown, so re-running the worker - which
 * WorkManager does every 15 minutes - never produces duplicates. The Firestore
 * document also feeds the in-app Notifications screen, so the reminder is kept
 * even when the system notification is dismissed or permission was denied.
 */
class DueCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val firestore = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()

        return try {
            // Equality-only query, matching the rest of the app (no composite index).
            val loans = Tasks.await(
                firestore.collection(Equipment.COLLECTION)
                    .whereEqualTo("borrowedBy", uid)
                    .get()
            ).documents.map { Equipment.from(it) }

            for (equipment in loans) {
                val type = LoanPolicy.pendingReminderType(equipment, now) ?: continue
                val notification = LoanPolicy.reminderNotification(equipment, uid, type, now)

                val existing = Tasks.await(
                    firestore.collection(AppNotification.COLLECTION)
                        .document(notification.id)
                        .get()
                )
                if (existing.exists()) continue // already reminded for this loan

                Tasks.await(SmartDockRepository.saveNotification(notification))
                if (NotificationPrefs.isEnabled(applicationContext)) {
                    Notifier.show(applicationContext, notification)
                }
            }
            Result.success()
        } catch (e: Exception) {
            // Offline or rules not deployed yet: try again on the next period.
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK = "smartdock_due_check"
        private const val IMMEDIATE_WORK = "smartdock_due_check_now"

        /** Idempotent: keeps the existing schedule if one is already registered. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DueCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Runs one check right away, e.g. when the dashboard opens or after a borrow. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DueCheckWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request
            )
        }

        fun cancel(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(PERIODIC_WORK)
            manager.cancelUniqueWork(IMMEDIATE_WORK)
        }
    }
}
