package com.example.data.repository

import com.example.domain.model.AttendanceRecord
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val records = firestore.collection("attendance_records")

    private fun todayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    suspend fun checkIn(internId: String, internName: String, sessionId: String): Result<Unit> = runCatching {
        val dateKey = todayKey()
        val existing = records
            .whereEqualTo("internId", internId)
            .whereEqualTo("dateKey", dateKey)
            .limit(1)
            .get()
            .awaitResult()
            .documents
            .firstOrNull()

        if (existing != null) {
            existing.reference.update(
                mapOf(
                    "internName" to internName,
                    "checkInSessionId" to sessionId,
                    "checkInTime" to Date(),
                    "status" to "Hadir"
                )
            ).awaitResult()
        } else {
            val docRef = records.document()
            val record = AttendanceRecord(
                id = docRef.id,
                internId = internId,
                internName = internName,
                dateKey = dateKey,
                checkInSessionId = sessionId,
                checkInTime = Date(),
                status = "Hadir"
            )
            docRef.set(record).awaitResult()
        }
    }

    suspend fun checkOut(internId: String, internName: String, sessionId: String): Result<Unit> = runCatching {
        val existing = records
            .whereEqualTo("internId", internId)
            .whereEqualTo("dateKey", todayKey())
            .limit(1)
            .get()
            .awaitResult()
            .documents
            .firstOrNull() ?: throw IllegalStateException("Belum ada absensi masuk hari ini")

        existing.reference.update(
            mapOf(
                "internName" to internName,
                "checkOutSessionId" to sessionId,
                "checkOutTime" to Date()
            )
        ).awaitResult()
    }

    fun observeTodayRecord(internId: String): Flow<AttendanceRecord?> = callbackFlow {
        val registration = records
            .whereEqualTo("internId", internId)
            .whereEqualTo("dateKey", todayKey())
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.firstOrNull()?.toObject(AttendanceRecord::class.java))
            }
        awaitClose { registration.remove() }
    }

    fun observeHistoryForIntern(internId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val registration = records
            .whereEqualTo("internId", internId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AttendanceRecord::class.java)
                    ?.sortedByDescending { it.dateKey }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeRecordsForSession(sessionId: String, sessionIdField: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val registration = records
            .whereEqualTo(sessionIdField, sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(AttendanceRecord::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun getRecordsInRange(startDateKey: String, endDateKey: String): Result<List<AttendanceRecord>> = runCatching {
        records
            .whereGreaterThanOrEqualTo("dateKey", startDateKey)
            .whereLessThanOrEqualTo("dateKey", endDateKey)
            .get()
            .awaitResult()
            .toObjects(AttendanceRecord::class.java)
            .sortedWith(compareBy({ it.dateKey }, { it.internName }))
    }

    suspend fun countTodayPresent(): Result<Long> = runCatching {
        records
            .whereEqualTo("dateKey", todayKey())
            .whereEqualTo("status", "Hadir")
            .count()
            .get(AggregateSource.SERVER)
            .awaitResult()
            .count
    }
}
