package com.example.presentation.admin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SessionRepository
import com.example.data.repository.UserRepository
import com.example.domain.model.AttendanceSession
import com.example.ui.components.GradientHeader
import com.example.ui.components.IconBadge
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.Background
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToCreateSession: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    onNavigateToManageInterns: () -> Unit,
    onNavigateToSessionHistory: () -> Unit,
    onNavigateToLaporan: () -> Unit,
    onLogout: () -> Unit
) {
    val sessionRepository = remember { SessionRepository() }
    val attendanceRepository = remember { AttendanceRepository() }
    val userRepository = remember { UserRepository() }

    val activeSessions by sessionRepository.observeActiveSessions().collectAsState(initial = emptyList())
    var activeInternCount by remember { mutableStateOf<Long?>(null) }
    var presentTodayCount by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        activeInternCount = userRepository.countActiveInterns().getOrNull()
        presentTodayCount = attendanceRepository.countTodayPresent().getOrNull()
    }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateSession,
                containerColor = Teal,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Buat Sesi", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GradientHeader(
                    title = "Dashboard Admin",
                    subtitle = "Kelola sesi & absensi peserta",
                    actions = {
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Peserta Aktif",
                        value = activeInternCount?.toString() ?: "-",
                        icon = Icons.Default.Groups,
                        iconTint = OceanBlue,
                        iconBg = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Hadir Hari Ini",
                        value = presentTodayCount?.toString() ?: "-",
                        icon = Icons.Default.CheckCircle,
                        iconTint = SuccessGreen,
                        iconBg = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionTile(
                        title = "Kelola Peserta",
                        icon = Icons.Default.ManageAccounts,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToManageInterns
                    )
                    QuickActionTile(
                        title = "Riwayat Sesi",
                        icon = Icons.Default.History,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSessionHistory
                    )
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    QuickActionTile(
                        title = "Rekap Laporan",
                        icon = Icons.Default.Assessment,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToLaporan
                    )
                }
            }

            item {
                Text(
                    "Sesi Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            if (activeSessions.isEmpty()) {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada sesi aktif", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(activeSessions) { session ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SessionCard(session = session, onClick = { onNavigateToSessionDetail(session.id) })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SessionCard(session: AttendanceSession, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val endTimeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    val isMasuk = session.type != "PULANG"

    SoftCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconBadge(
                icon = if (isMasuk) Icons.Default.Login else Icons.Default.Logout,
                tint = if (isMasuk) Teal else Color(0xFFF57C00),
                background = if (isMasuk) Color(0xFFE6F7F7) else Color(0xFFFFF3E0)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sesi ${session.type}", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(timeFormatter.format(session.startTime), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("s/d ${endTimeFormatter.format(session.endTime)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            StatusChip(text = session.status, color = SuccessGreen, background = Color(0xFFF0FDF4))
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SoftCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconBadge(icon = icon, tint = OceanBlue, background = Color(0xFFEFF6FF), size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier) {
        IconBadge(icon = icon, tint = iconTint, background = iconBg)
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
