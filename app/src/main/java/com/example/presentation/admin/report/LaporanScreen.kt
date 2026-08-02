package com.example.presentation.admin.report

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.repository.AttendanceRepository
import com.example.domain.model.AttendanceRecord
import com.example.ui.components.GradientHeader
import com.example.ui.components.PrimaButton
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.Background
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attendanceRepository = remember { AttendanceRepository() }

    val utcKeyFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
    val displayDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

    var startMillis by remember { mutableStateOf(utcMidnightDaysAgo(6)) }
    var endMillis by remember { mutableStateOf(utcMidnightDaysAgo(0)) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var records by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun runQuery() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val startKey = utcKeyFormatter.format(Date(startMillis))
            val endKey = utcKeyFormatter.format(Date(endMillis))
            attendanceRepository.getRecordsInRange(startKey, endKey)
                .onSuccess { records = it }
                .onFailure { errorMessage = it.message ?: "Gagal memuat data" }
            isLoading = false
            hasSearched = true
        }
    }

    LaunchedEffect(Unit) { runQuery() }

    val totalHadir = records.count { it.checkInTime != null }

    Scaffold(containerColor = Background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(
                title = "Rekap Laporan",
                subtitle = "Riwayat kehadiran peserta",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        if (records.isNotEmpty()) {
                            shareAsCsv(context, records, timeFormatter)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan CSV", tint = Color.White)
                    }
                }
            )

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateFieldButton(
                        label = "Dari",
                        text = displayDateFormatter.format(Date(startMillis)),
                        modifier = Modifier.weight(1f),
                        onClick = { showStartPicker = true }
                    )
                    DateFieldButton(
                        label = "Sampai",
                        text = displayDateFormatter.format(Date(endMillis)),
                        modifier = Modifier.weight(1f),
                        onClick = { showEndPicker = true }
                    )
                }

                PrimaButton(
                    text = "Tampilkan",
                    isLoading = isLoading,
                    containerColor = OceanBlue,
                    onClick = { runQuery() }
                )

                if (errorMessage != null) {
                    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                    }
                }

                if (hasSearched && !isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SoftCard(modifier = Modifier.weight(1f)) {
                            Text("Total Data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(records.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        SoftCard(modifier = Modifier.weight(1f)) {
                            Text("Hadir", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(totalHadir.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal)
                }
            } else if (hasSearched && records.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak ada data di rentang tanggal ini", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records) { record ->
                        ReportRow(record = record, dateFormatter = displayDateFormatter, timeFormatter = timeFormatter)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startMillis = it }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endMillis = it }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateFieldButton(label: String, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SoftCard(modifier = modifier, onClick = onClick) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Teal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
private fun ReportRow(record: AttendanceRecord, dateFormatter: SimpleDateFormat, timeFormatter: SimpleDateFormat) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.internName.ifBlank { record.internId }, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    try {
                        dateFormatter.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.dateKey) ?: Date())
                    } catch (e: Exception) {
                        record.dateKey
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
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

private fun utcMidnightDaysAgo(days: Int): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    return calendar.timeInMillis
}

private fun shareAsCsv(context: Context, records: List<AttendanceRecord>, timeFormatter: SimpleDateFormat) {
    val builder = StringBuilder()
    builder.append("Nama,ID Peserta,Tanggal,Jam Masuk,Jam Pulang,Status\n")
    records.forEach { record ->
        val name = record.internName.ifBlank { record.internId }.replace("\"", "'")
        builder.append("\"$name\",${record.internId},${record.dateKey},")
        builder.append("${record.checkInTime?.let { timeFormatter.format(it) } ?: "-"},")
        builder.append("${record.checkOutTime?.let { timeFormatter.format(it) } ?: "-"},")
        builder.append("${record.status}\n")
    }

    val file = File(context.cacheDir, "rekap_absensi_${System.currentTimeMillis()}.csv")
    file.writeText(builder.toString())

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Rekap Absensi"))
}
