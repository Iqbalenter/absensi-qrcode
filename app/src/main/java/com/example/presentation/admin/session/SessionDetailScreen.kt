package com.example.presentation.admin.session

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SessionRepository
import com.example.ui.components.GradientHeader
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.Background
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sessionRepository = remember { SessionRepository() }
    val attendanceRepository = remember { AttendanceRepository() }

    val session by sessionRepository.observeSession(sessionId).collectAsState(initial = null)
    val sessionIdField = if (session?.type == "PULANG") "checkOutSessionId" else "checkInSessionId"
    val attendees by attendanceRepository.observeRecordsForSession(sessionId, sessionIdField)
        .collectAsState(initial = emptyList())

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var countdown by remember { mutableStateOf(30) }
    val timeFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }
    val endTimeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

    LaunchedEffect(session?.qrToken) {
        val token = session?.qrToken ?: return@LaunchedEffect
        val size = 512
        val bitMatrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        qrBitmap = bitmap
    }

    LaunchedEffect(sessionId, session?.status) {
        while (session?.status == "AKTIF") {
            delay(1000)
            if (countdown > 0) {
                countdown--
            } else {
                sessionRepository.rotateQrToken(sessionId)
                countdown = 30
            }
        }
    }

    Scaffold(containerColor = Background) { padding ->
        val currentSession = session
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(
                title = "QR Code Sesi",
                subtitle = currentSession?.let { "Sesi ${it.type}" },
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            sessionRepository.rotateQrToken(sessionId)
                            countdown = 30
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Perbarui QR", tint = Color.White)
                    }
                }
            )

            if (currentSession == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "${timeFormatter.format(currentSession.startTime)} - ${endTimeFormatter.format(currentSession.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            if (currentSession.status == "AKTIF") {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "QR Code Sesi",
                                        modifier = Modifier.size(260.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                StatusChip(
                                    text = "Diperbarui dalam ${countdown}d",
                                    color = Teal,
                                    background = SoftCyan
                                )
                            } else {
                                Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                                    Text("Sesi ini sudah ditutup", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "${attendees.size} orang sudah absen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(attendees) { record ->
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(SoftCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                record.internName.ifBlank { record.internId },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            StatusChip(text = record.status, color = SuccessGreen, background = Color(0xFFF0FDF4))
                        }
                    }
                }

                if (currentSession.status == "AKTIF") {
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    sessionRepository.closeSession(sessionId)
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Tutup Sesi", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
