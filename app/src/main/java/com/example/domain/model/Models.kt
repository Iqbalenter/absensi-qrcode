package com.example.domain.model

import java.util.Date

enum class Role { ADMIN, HRD, MAGANG }

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: Role = Role.MAGANG,
    val nim: String = "",
    val status: String = "aktif"
)

data class AttendanceSession(
    val id: String = "",
    val type: String = "MASUK",
    val dateKey: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val qrToken: String = "",
    val status: String = "TERJADWAL",
    val createdBy: String = "",
    val description: String = ""
)

data class AttendanceRecord(
    val id: String = "",
    val internId: String = "",
    val internName: String = "",
    val dateKey: String = "",
    val checkInSessionId: String? = null,
    val checkOutSessionId: String? = null,
    val checkInTime: Date? = null,
    val checkOutTime: Date? = null,
    val status: String = "Hadir",
    val description: String = ""
)
