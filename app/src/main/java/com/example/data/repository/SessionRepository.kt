package com.example.data.repository

import com.example.domain.model.AttendanceSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val sessions = firestore.collection("sessions")

    suspend fun createSession(
        type: String,
        description: String,
        durationMinutes: Long = 60
    ): Result<String> = runCatching {
        val now = Date()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val docRef = sessions.document()
        val session = AttendanceSession(
            id = docRef.id,
            type = type,
            dateKey = dateKey,
            startTime = now,
            endTime = Date(now.time + durationMinutes * 60_000),
            qrToken = UUID.randomUUID().toString(),
            status = "AKTIF",
            createdBy = auth.currentUser?.uid ?: "",
            description = description
        )
        docRef.set(session).awaitResult()
        docRef.id
    }

    fun observeSession(sessionId: String): Flow<AttendanceSession?> = callbackFlow {
        val registration = sessions.document(sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(AttendanceSession::class.java))
        }
        awaitClose { registration.remove() }
    }

    fun observeActiveSessions(): Flow<List<AttendanceSession>> = callbackFlow {
        val registration = sessions
            .whereEqualTo("status", "AKTIF")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AttendanceSession::class.java)
                    ?.sortedByDescending { it.startTime }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeAllSessions(): Flow<List<AttendanceSession>> = callbackFlow {
        val registration = sessions.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(AttendanceSession::class.java)
                ?.sortedByDescending { it.startTime }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun rotateQrToken(sessionId: String): Result<String> = runCatching {
        val newToken = UUID.randomUUID().toString()
        sessions.document(sessionId).update("qrToken", newToken).awaitResult()
        newToken
    }

    suspend fun closeSession(sessionId: String): Result<Unit> = runCatching {
        sessions.document(sessionId).update("status", "SELESAI").awaitResult()
    }

    suspend fun findActiveSessionByToken(token: String): Result<AttendanceSession> = runCatching {
        val snapshot = sessions
            .whereEqualTo("status", "AKTIF")
            .whereEqualTo("qrToken", token)
            .limit(1)
            .get()
            .awaitResult()
        snapshot.documents.firstOrNull()?.toObject(AttendanceSession::class.java)
            ?: throw IllegalStateException("QR Code tidak valid atau sesi sudah berakhir")
    }
}
