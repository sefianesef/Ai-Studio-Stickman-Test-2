package com.mygames.stickmanrush.security

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthService(private val context: Context) {
    companion object {
        private const val TAG = "FirebaseAuthService"
    }

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(FirebaseCloudWalletService.FIREBASE_PROJECT_ID)
                    .setApplicationId("1:842418078736:android:b891ca82349071d2")
                    .setApiKey("AIzaSyStickmanRushProjectKey88867")
                    .setStorageBucket("${FirebaseCloudWalletService.FIREBASE_PROJECT_ID}.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseApp init warning: ${e.message}")
        }
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val currentUser: FirebaseUser?
        get() = try { auth.currentUser } catch (e: Exception) { null }

    suspend fun registerWithEmail(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw IllegalStateException("User UID is null")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw IllegalStateException("User UID is null")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureAuthenticated(): String = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user != null) {
            return@withContext user.uid
        }
        val result = auth.signInAnonymously().await()
        val newUid = result.user?.uid ?: throw Exception("Anonymous auth failed")
        return@withContext newUid
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }
}
