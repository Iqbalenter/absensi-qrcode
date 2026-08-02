package com.example.presentation.admin.interns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.repository.UserRepository
import com.example.domain.model.User
import com.example.ui.components.GradientHeader
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.Background
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInternsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }

    val interns by userRepository.observeAllInterns().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<User?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Teal, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Peserta")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(title = "Kelola Peserta", subtitle = "${interns.size} peserta magang terdaftar", onBack = onBack)

            if (actionError != null) {
                Surface(color = Color(0xFFFEF2F2), modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(actionError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }

            if (interns.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.GroupOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada peserta magang", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(interns) { intern ->
                        InternRow(
                            intern = intern,
                            onToggleStatus = {
                                scope.launch {
                                    userRepository.setInternStatus(intern.uid, intern.status != "aktif")
                                        .onFailure { actionError = it.message }
                                }
                            },
                            onDelete = { deleteTarget = intern }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInternDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { name, email, password, nim ->
                scope.launch {
                    userRepository.createIntern(context, name, email, password, nim)
                        .onSuccess { showAddDialog = false }
                        .onFailure { actionError = it.message ?: "Gagal menambahkan peserta" }
                }
            }
        )
    }

    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Peserta?") },
            text = { Text("Data ${target.name} akan dihapus dari daftar peserta. Akun login yang bersangkutan tidak akan bisa dipakai lagi (data profil hilang) walau kredensialnya tidak terhapus penuh dari sistem otentikasi.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        userRepository.deleteIntern(target.uid).onFailure { actionError = it.message }
                        deleteTarget = null
                    }
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun InternRow(intern: User, onToggleStatus: () -> Unit, onDelete: () -> Unit) {
    val isActive = intern.status == "aktif"
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(intern.name.take(1).uppercase().ifEmpty { "?" }, color = Teal, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(intern.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("NIM: ${intern.nim}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(intern.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatusChip(
                text = if (isActive) "Aktif" else "Nonaktif",
                color = if (isActive) SuccessGreen else Color(0xFF64748B),
                background = if (isActive) Color(0xFFF0FDF4) else Color(0xFFF1F5F9)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onToggleStatus) {
                Icon(
                    if (isActive) Icons.Default.PersonOff else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isActive) "Nonaktifkan" else "Aktifkan")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddInternDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, password: String, nim: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Peserta Magang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nim, onValueChange = { nim = it }, label = { Text("NIM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min. 6 karakter)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() || email.isBlank() || password.isBlank() || nim.isBlank() ->
                        error = "Semua kolom wajib diisi"
                    password.length < 6 ->
                        error = "Password minimal 6 karakter"
                    else -> onSubmit(name.trim(), email.trim(), password, nim.trim())
                }
            }) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
