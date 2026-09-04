package com.example.eldroid_nullpoint.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Tiny service locator so Activities can obtain the shared repositories
 * without constructing Firebase objects themselves. Presenters receive the
 * repositories through their constructor, which is what lets tests pass fakes.
 */
object Injection {

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var equipmentRepository: EquipmentRepository? = null

    fun provideAuthRepository(context: Context): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: FirebaseAuthRepository(
                appContext = context.applicationContext,
                auth = FirebaseAuth.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            ).also { authRepository = it }
        }

    fun provideEquipmentRepository(): EquipmentRepository =
        equipmentRepository ?: synchronized(this) {
            equipmentRepository ?: FirestoreEquipmentRepository(FirebaseFirestore.getInstance())
                .also { equipmentRepository = it }
        }
}
