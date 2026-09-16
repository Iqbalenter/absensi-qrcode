package com.example.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.components.PrimaButton
import com.example.ui.components.SoftCard
import com.example.ui.theme.Background
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.Teal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEmailSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to DeepNavy,
                    0.45f to DeepNavy,
                    0.45f to Background,
                    1f to Background
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = SoftCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Lupa Password",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Kami akan kirimkan link reset ke email Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftCyan.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 28.dp)
        )

        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            if (isEmailSent) {
                Text(
                    "Email Terkirim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Kami telah mengirimkan link untuk reset password ke $email. " +
                        "Silakan cek kotak masuk (atau folder spam) dan ikuti instruksinya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaButton(
                    text = "Kembali ke Login",
                    containerColor = OceanBlue,
                    onClick = onBack
                )
            } else {
                Text(
                    "Masukkan Email",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal,
                    focusedLabelColor = Teal,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Teal,
                    focusedLeadingIconColor = Teal,
                    unfocusedLeadingIconColor = TextSecondary
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaButton(
                    text = "Kirim Link Reset",
                    isLoading = isLoading,
                    containerColor = OceanBlue,
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "Email tidak boleh kosong"
                        } else {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                authRepository.sendPasswordReset(email.trim())
                                    .onSuccess { isEmailSent = true }
                                    .onFailure { errorMessage = it.toResetErrorMessage() }
                                isLoading = false
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onBack) {
                    Text("Kembali ke Login", color = OceanBlue)
                }
            }
        }
    }
}

private fun Throwable.toResetErrorMessage(): String = when (this) {
    is FirebaseAuthInvalidUserException -> "Akun dengan email ini tidak ditemukan"
    is FirebaseNetworkException -> "Tidak ada koneksi internet, coba lagi"
    else -> message ?: "Terjadi kesalahan, coba lagi"
}
