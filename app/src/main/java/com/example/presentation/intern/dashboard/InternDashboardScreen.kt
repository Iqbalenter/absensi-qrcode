package com.example.presentation.intern.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.UserRepository
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.User
import com.example.ui.components.IconBadge
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternDashboardScreen(
    onNavigateToScanner: () -> Unit,
    onLogout: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val userRepository = remember { UserRepository() }
    val attendanceRepository = remember { AttendanceRepository() }
    var selectedTab by remember { mutableStateOf(0) }

    val user by (uid?.let { userRepository.observeUser(it) } ?: emptyFlowOfUser())
        .collectAsState(initial = null)
    val todayRecord by (uid?.let { attendanceRepository.observeTodayRecord(it) } ?: emptyFlowOfRecord())
        .collectAsState(initial = null)
    val history by (uid?.let { attendanceRepository.observeHistoryForIntern(it) } ?: emptyFlowOfRecordList())
        .collectAsState(initial = emptyList())

    val doLogout = {
        FirebaseAuth.getInstance().signOut()
        onLogout()
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HeaderSection(onLogout = doLogout)

            when (selectedTab) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    GreetingSection(user = user)
                    StatusCardsSection(record = todayRecord)
                    ScannerSection(modifier = Modifier.weight(1f), onClick = onNavigateToScanner)
                    RecentHistorySection(record = todayRecord)
                }
                1 -> HistoryTab(history = history)
                else -> ProfileTab(user = user, onLogout = doLogout)
            }
        }
    }
}

private fun emptyFlowOfUser() = kotlinx.coroutines.flow.flowOf<User?>(null)
private fun emptyFlowOfRecord() = kotlinx.coroutines.flow.flowOf<AttendanceRecord?>(null)
private fun emptyFlowOfRecordList() = kotlinx.coroutines.flow.flowOf<List<AttendanceRecord>>(emptyList())

@Composable
fun HeaderSection(onLogout: () -> Unit) {
    val dateText = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(DeepNavy, Color(0xFF0D2840))))
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PRIMA ATTENDANCE",
                    color = SoftCyan.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Text(
                    "Dashboard Peserta",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, OceanBlue, CircleShape)
                    .background(OceanBlue)
                    .clickable { onLogout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Teal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(dateText, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                Text("PT. Prima Multi Peti Kemas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GreetingSection(user: User?) {
    Column {
        Text("Halo, ${user?.name ?: "Peserta"} 👋", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("NIM: ${user?.nim ?: "-"}", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun StatusCardsSection(record: AttendanceRecord?) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "JAM MASUK",
            time = record?.checkInTime?.let { timeFormatter.format(it) } ?: "-- : --",
            timeSuffix = if (record?.checkInTime != null) "WIB" else "",
            status = if (record?.checkInTime != null) "Sudah Absen" else "Belum Absen",
            statusColor = if (record?.checkInTime != null) Color(0xFF15803D) else Color(0xFF64748B),
            statusBg = if (record?.checkInTime != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
            timeColor = if (record?.checkInTime != null) Teal else Color(0xFFCBD5E1)
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "JAM PULANG",
            time = record?.checkOutTime?.let { timeFormatter.format(it) } ?: "-- : --",
            timeSuffix = if (record?.checkOutTime != null) "WIB" else "",
            status = if (record?.checkOutTime != null) "Sudah Absen" else "Belum Sesi",
            statusColor = if (record?.checkOutTime != null) Color(0xFF15803D) else Color(0xFF64748B),
            statusBg = if (record?.checkOutTime != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
            timeColor = if (record?.checkOutTime != null) Teal else Color(0xFFCBD5E1)
        )
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    time: String,
    timeSuffix: String,
    status: String,
    statusColor: Color,
    statusBg: Color,
    timeColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, color = timeColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (timeSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(timeSuffix, color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ScannerSection(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Teal.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(Teal.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size((160 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(Teal.copy(alpha = pulseAlpha))
            )

            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .clickable { onClick() },
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 16.dp,
                border = BorderStroke(4.dp, Color.White)
            ) {
                Box(
                    modifier = Modifier.background(Brush.linearGradient(listOf(OceanBlue, DeepNavy))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "SCAN QR",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Arahkan kamera ke QR Sesi",
            color = OceanBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentHistorySection(record: AttendanceRecord?) {
    val timeFormatter = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AKTIVITAS HARI INI",
                    color = DeepNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (record == null) {
                Text("Belum ada aktivitas hari ini", fontSize = 12.sp, color = TextSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Absensi ${record.status}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        val lastTime = record.checkOutTime ?: record.checkInTime
                        Text(
                            lastTime?.let { timeFormatter.format(it) } ?: "-",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryTab(history: List<AttendanceRecord>) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

    if (history.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Belum ada riwayat absensi", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(history) { record ->
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            try {
                                dateFormatter.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.dateKey) ?: Date())
                            } catch (e: Exception) {
                                record.dateKey
                            },
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Masuk: ${record.checkInTime?.let { timeFormatter.format(it) } ?: "-"}   Pulang: ${record.checkOutTime?.let { timeFormatter.format(it) } ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StatusChip(text = record.status, color = SuccessGreen, background = Color(0xFFF0FDF4))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProfileTab(user: User?, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(SoftCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user?.name?.take(1)?.uppercase() ?: "?",
                        color = Teal,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(user?.name ?: "-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(user?.email ?: "-", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                StatusChip(
                    text = if (user?.status == "aktif") "Aktif" else "Nonaktif",
                    color = if (user?.status == "aktif") SuccessGreen else Color(0xFF64748B),
                    background = if (user?.status == "aktif") Color(0xFFF0FDF4) else Color(0xFFF1F5F9)
                )
            }
        }

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            ProfileInfoRow(icon = Icons.Default.Badge, label = "NIM", value = user?.nim ?: "-")
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "-")
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(icon = Icons.Default.VerifiedUser, label = "Role", value = user?.role?.name ?: "-")
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconBadge(icon = icon, tint = Teal, background = SoftCyan, size = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Beranda",
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.History,
                label = "Riwayat",
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profil",
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftCyan)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Icon(icon, contentDescription = label, tint = OceanBlue)
            }
        } else {
            Icon(icon, contentDescription = label, tint = TextSecondary.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OceanBlue else TextSecondary.copy(alpha = 0.5f)
        )
    }
}
