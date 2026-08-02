package com.example.presentation.intern.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SessionRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.GradientHeader
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Teal
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sessionRepository = remember { SessionRepository() }
    val attendanceRepository = remember { AttendanceRepository() }
    val userRepository = remember { UserRepository() }

    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedValue by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedValue) {
        val token = scannedValue
        if (token != null && !isValidating && !scanSuccess) {
            isValidating = true
            scanError = null

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                scanError = "Sesi login berakhir, silakan masuk kembali"
                isValidating = false
                delay(1500)
                scannedValue = null
                return@LaunchedEffect
            }

            val sessionResult = sessionRepository.findActiveSessionByToken(token)
            val session = sessionResult.getOrNull()
            if (session == null) {
                scanError = sessionResult.exceptionOrNull()?.message ?: "QR Code tidak valid"
                isValidating = false
                delay(1500)
                scannedValue = null
                return@LaunchedEffect
            }

            val internName = userRepository.getUser(uid).getOrNull()?.name ?: ""
            val attendanceResult = if (session.type == "PULANG") {
                attendanceRepository.checkOut(uid, internName, session.id)
            } else {
                attendanceRepository.checkIn(uid, internName, session.id)
            }

            isValidating = false
            attendanceResult
                .onSuccess {
                    scanSuccess = true
                    delay(1000)
                    onScanSuccess()
                }
                .onFailure {
                    scanError = it.message ?: "Gagal mencatat kehadiran"
                    delay(1500)
                    scannedValue = null
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isValidating && !scanSuccess && scannedValue == null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty()) {
                                            scannedValue = barcodes[0].rawValue
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder frame
            if (!scanSuccess && scanError == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .background(Color.Transparent, RoundedCornerShape(24.dp))
                        .border(BorderStroke(3.dp, Teal), RoundedCornerShape(24.dp))
                )
            }

            GradientHeader(
                title = "Scan QR Sesi",
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (isValidating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Teal)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Memvalidasi QR...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else if (scanSuccess) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Absensi Berhasil!", color = SuccessGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            } else if (scanError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(scanError!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "Arahkan kamera ke QR Code Sesi",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Text(
                "Izin kamera diperlukan untuk fitur ini.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}
