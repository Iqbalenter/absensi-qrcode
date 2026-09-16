package com.example.data.repository

import com.example.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String): Result<User> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, password).awaitResult()
        val uid = authResult.user?.uid
            ?: throw IllegalStateException("UID pengguna tidak ditemukan")

        val snapshot = firestore.collection("users").document(uid).get().awaitResult()

        val user = snapshot.toObject(User::class.java)?.copy(uid = uid)
            ?: throw IllegalStateException("Data pengguna tidak ditemukan di Firestore")

        if (user.status != "aktif") {
            auth.signOut()
            throw IllegalStateException("Akun Anda tidak aktif, silakan hubungi admin")
        }

        user
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).awaitResult()
        Unit
    }
}
