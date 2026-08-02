package com.example.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = SoftCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Prima Attendance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "PT. Prima Multi Peti Kemas",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftCyan.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 28.dp)
        )

        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            Text(
                "Masuk ke Akun",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Card ini selalu berlatar putih, jadi warna teks/ikon dikunci gelap
            // supaya tetap terbaca walau HP memakai dark theme (tidak ikut MaterialTheme).
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                focusedLabelColor = Teal,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Teal,
                focusedLeadingIconColor = Teal,
                unfocusedLeadingIconColor = TextSecondary,
                focusedTrailingIconColor = Teal,
                unfocusedTrailingIconColor = TextSecondary
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

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                }
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
                text = "Masuk",
                isLoading = isLoading,
                containerColor = OceanBlue,
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email atau Password tidak boleh kosong"
                    } else {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            authRepository.login(email.trim(), password)
                                .onSuccess { user -> onLoginSuccess(user.role.name) }
                                .onFailure { errorMessage = it.toLoginErrorMessage() }
                            isLoading = false
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Absensi digital berbasis QR Code",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

private fun Throwable.toLoginErrorMessage(): String = when (this) {
    is FirebaseAuthInvalidUserException -> "Akun dengan email ini tidak ditemukan"
    is FirebaseAuthInvalidCredentialsException -> "Email atau password salah"
    is FirebaseNetworkException -> "Tidak ada koneksi internet, coba lagi"
    else -> message ?: "Terjadi kesalahan, coba lagi"
}
