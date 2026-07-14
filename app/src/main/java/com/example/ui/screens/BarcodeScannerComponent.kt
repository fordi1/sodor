package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.utils.PersianDigitConverter
import com.example.ui.theme.LocalIsDarkTheme
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerLauncher(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        BarcodeScannerScreen(onBarcodeScanned = onBarcodeScanned, onDismiss = onDismiss)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Card(modifier = Modifier.padding(32.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("برای اسکن بارکد، دسترسی به دوربین لازم است.", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("درخواست مجوز")
                        }
                        OutlinedButton(onClick = onDismiss) {
                            Text("انصراف")
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasScanned by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39
                )
                .build()
            
            val scanner = BarcodeScanning.getClient(options)
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (hasScanned) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (hasScanned) return@addOnSuccessListener
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            hasScanned = true
                                            
                                            // Feedback
                                            try {
                                                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                            } catch (e: Exception) { }
                    
                                            try {
                                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                                if (vibrator != null) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator.vibrate(100)
                                                    }
                                                }
                                            } catch (e: Exception) { }

                                            val finalBarcode = PersianDigitConverter.toEnglish(rawValue).trim()
                                            Log.d("BarcodeScanner", "Barcode detected: $finalBarcode")
                                            
                                            imageProxy.close()
                                            onBarcodeScanned(finalBarcode)
                                            return@addOnSuccessListener
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Log.e("BarcodeScanner", "Failed to scan", it)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                // Set initial flash state
                camera?.cameraControl?.enableTorch(flashEnabled)
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Failed to unbind camera", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { 
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Set tap to focus logic
                    setOnTouchListener { view, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                            view.performClick()
                        }
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark Overlay with transparent cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val rectWidth = canvasWidth * 0.75f
            val rectHeight = rectWidth // Square for QR/Barcodes
            val left = (canvasWidth - rectWidth) / 2
            val top = (canvasHeight - rectHeight) / 2

            // Background dark
            drawRect(color = Color.Black.copy(alpha = 0.6f), size = size)
            
            // Clear cutout
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        // Overlay scanning frame box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .border(2.dp, if (isDark) Color(0xFF34D399) else Color(0xFF10B981), RoundedCornerShape(16.dp))
        )

        // Title and helper text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "اسکن بارکد",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "بارکد را داخل کادر قرار دهید و دوربین را ثابت نگه دارید.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Actions
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            
            IconButton(
                onClick = { 
                    flashEnabled = !flashEnabled 
                    camera?.cameraControl?.enableTorch(flashEnabled)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(if(flashEnabled) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else Color.Black.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            ) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, 
                    contentDescription = "فلش", 
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

