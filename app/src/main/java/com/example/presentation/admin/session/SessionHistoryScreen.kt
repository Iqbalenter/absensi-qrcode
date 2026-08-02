package com.example.presentation.admin.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.SessionRepository
import com.example.presentation.admin.dashboard.SessionCard
import com.example.ui.components.GradientHeader
import com.example.ui.theme.Background
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit
) {
    val sessionRepository = remember { SessionRepository() }
    val allSessions by sessionRepository.observeAllSessions().collectAsState(initial = emptyList())

    Scaffold(containerColor = Background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(title = "Riwayat Sesi", subtitle = "${allSessions.size} sesi tercatat", onBack = onBack)

            if (allSessions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada sesi", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allSessions) { session ->
                        SessionCard(session = session, onClick = { onSessionClick(session.id) })
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}
