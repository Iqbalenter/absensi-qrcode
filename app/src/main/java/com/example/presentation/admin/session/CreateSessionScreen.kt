package com.example.presentation.admin.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SessionRepository
import com.example.ui.components.GradientHeader
import com.example.ui.components.IconBadge
import com.example.ui.components.PrimaButton
import com.example.ui.theme.Background
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    onBack: () -> Unit,
    onSessionCreated: (String) -> Unit
) {
    var sessionType by remember { mutableStateOf("MASUK") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sessionRepository = remember { SessionRepository() }

    Scaffold(containerColor = Background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(title = "Buat Sesi Baru", subtitle = "Pilih jenis sesi absensi", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Jenis Sesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

                SessionTypeOption(
                    title = "Sesi Masuk",
                    description = "Absensi kedatangan peserta",
                    icon = Icons.Default.Login,
                    color = Teal,
                    selected = sessionType == "MASUK",
                    onClick = { sessionType = "MASUK" }
                )

                SessionTypeOption(
                    title = "Sesi Pulang",
                    description = "Absensi kepulangan peserta",
                    icon = Icons.Default.Logout,
                    color = Color(0xFFF57C00),
                    selected = sessionType == "PULANG",
                    onClick = { sessionType = "PULANG" }
                )

                if (errorMessage != null) {
                    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                PrimaButton(
                    text = "Buat & Tampilkan QR",
                    isLoading = isLoading,
                    containerColor = OceanBlue,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            sessionRepository.createSession(type = sessionType, description = "")
                                .onSuccess { sessionId -> onSessionCreated(sessionId) }
                                .onFailure { errorMessage = it.message ?: "Gagal membuat sesi, coba lagi" }
                            isLoading = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionTypeOption(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(2.dp, if (selected) color else Color(0xFFE5E9EF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = icon, tint = color, background = color.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = color))
        }
    }
}
