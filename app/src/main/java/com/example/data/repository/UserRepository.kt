package com.example.data.repository

import android.content.Context
import com.example.domain.model.Role
import com.example.domain.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val users = firestore.collection("users")

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(User::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun getUser(uid: String): Result<User> = runCatching {
        users.document(uid).get().awaitResult().toObject(User::class.java)
            ?: throw IllegalStateException("Data pengguna tidak ditemukan")
    }

    fun observeAllInterns(): Flow<List<User>> = callbackFlow {
        val registration = users
            .whereEqualTo("role", "MAGANG")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(User::class.java)?.sortedBy { it.name } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun countActiveInterns(): Result<Long> = runCatching {
        users
            .whereEqualTo("role", "MAGANG")
            .whereEqualTo("status", "aktif")
            .count()
            .get(AggregateSource.SERVER)
            .awaitResult()
            .count
    }

    /**
     * Membuat akun magang baru lewat FirebaseApp sekunder, supaya sesi login admin
     * yang sedang aktif tidak ikut tergantikan oleh akun baru ini.
     */
    suspend fun createIntern(
        context: Context,
        name: String,
        email: String,
        password: String,
        nim: String
    ): Result<String> = runCatching {
        val secondaryApp = FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            "CreateUser-${System.currentTimeMillis()}"
        ) ?: throw IllegalStateException("Gagal menyiapkan proses pembuatan akun")

        try {
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).awaitResult()
            val uid = authResult.user?.uid ?: throw IllegalStateException("Gagal membuat akun")

            authResult.user
                ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                ?.awaitResult()

            secondaryAuth.signOut()

            users.document(uid).set(
                User(uid = uid, name = name, email = email, role = Role.MAGANG, nim = nim, status = "aktif")
            ).awaitResult()

            uid
        } finally {
            secondaryApp.delete()
        }
    }

    suspend fun setInternStatus(uid: String, active: Boolean): Result<Unit> = runCatching {
        users.document(uid).update("status", if (active) "aktif" else "nonaktif").awaitResult()
    }

    suspend fun deleteIntern(uid: String): Result<Unit> = runCatching {
        users.document(uid).delete().awaitResult()
    }
}
