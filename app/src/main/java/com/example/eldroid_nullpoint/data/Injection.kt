package com.example.eldroid_nullpoint.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Tiny service locator so Activities can obtain the shared [AuthRepository]
 * without constructing Firebase objects themselves. Presenters receive the
 * repository through their constructor, which is what lets tests pass a fake.
 */
object Injection {

    @Volatile
    private var authRepository: AuthRepository? = null

    fun provideAuthRepository(context: Context): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: FirebaseAuthRepository(
                appContext = context.applicationContext,
                auth = FirebaseAuth.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            ).also { authRepository = it }
        }
}
