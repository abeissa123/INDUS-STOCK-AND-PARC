package com.example.ui.components

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

data class ScannableItem(
    val code: String,
    val name: String,
    val category: String
)

@androidx.annotation.OptIn(ExperimentalGetImage::class)
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()
    private var lastScannedTime = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val currentTime = System.currentTimeMillis()
            // Throttle detection to once every 1200ms to avoid spamming results
            if (currentTime - lastScannedTime > 1200) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrBlank()) {
                                lastScannedTime = currentTime
                                onBarcodeDetected(rawValue)
                                break
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Fail silently
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val executor = ContextCompat.getMainExecutor(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(executor, BarcodeAnalyzer(onBarcodeDetected))
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerDialog(
    title: String = "Scanner d'Outils & Équipements",
    subtitle: String = "Pointez l'appareil photo vers le code-barres ou sélectionnez un outil ci-dessous",
    scannableItems: List<ScannableItem>,
    onDismiss: () -> Unit,
    onScanResult: (String) -> Unit
) {
    var scanMode by remember { mutableStateOf("camera") } // "camera" or "simulated"
    var searchQuery by remember { mutableStateOf("") }
    var manualCode by remember { mutableStateOf("") }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Laser Animation setup for the scanning viewport HUD overlay
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Filtering items for the interactive simulation list
    val filteredItems = remember(scannableItems, searchQuery) {
        if (searchQuery.isBlank()) {
            scannableItems.take(5)
        } else {
            scannableItems.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var scannedCodeFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedCodeFeedback) {
        if (scannedCodeFeedback != null) {
            delay(1000) // visual feedback delay showing success animation
            onScanResult(scannedCodeFeedback!!)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Close button & title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Modes Tab Selection Row
                TabRow(
                    selectedTabIndex = if (scanMode == "camera") 0 else 1,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = scanMode == "camera",
                        onClick = { scanMode = "camera" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Caméra Réelle", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = scanMode == "simulated",
                        onClick = { scanMode = "simulated" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Simulation & Saisie", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Scan Area / Viewport Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (scanMode == "camera") {
                        if (cameraPermissionState.status.isGranted) {
                            // Real camera preview using CameraX and ML Kit BarcodeAnalyzer
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                onBarcodeDetected = { barcode ->
                                    if (scannedCodeFeedback == null) {
                                        scannedCodeFeedback = barcode
                                    }
                                }
                            )
                        } else {
                            // Camera Permission request banner
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Permission de la Caméra Requise",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pour scanner des codes QR ou codes-barres physiques, l'application doit accéder à l'appareil photo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Autoriser l'appareil photo")
                                }
                            }
                        }
                    } else {
                        // Simulated Viewfinder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF020617))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Mode Simulation Actif",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Utilisez la liste ou le clavier ci-dessous",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Viewport HUD (Drawn over camera preview or simulation screen)
                    if (scanMode == "simulated" || (scanMode == "camera" && cameraPermissionState.status.isGranted)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // Framing target dimensions (centered box)
                            val frameWidth = canvasWidth * 0.75f
                            val frameHeight = canvasHeight * 0.55f
                            val left = (canvasWidth - frameWidth) / 2
                            val top = (canvasHeight - frameHeight) / 2
                            val right = left + frameWidth
                            val bottom = top + frameHeight

                            // Dark overlay around target window
                            drawRect(
                                color = Color.Black.copy(alpha = 0.4f),
                                size = size
                            )

                            // Frame border
                            drawRect(
                                color = Color.White.copy(alpha = 0.3f),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                )
                            )

                            // Corner bracket highlights (Neon Green)
                            val cornerLen = 22.dp.toPx()
                            val strokeW = 4.dp.toPx()
                            val neonGreen = Color(0xFF10B981)

                            // Top-Left corner bracket
                            drawLine(neonGreen, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                            drawLine(neonGreen, Offset(left, top), Offset(left, top + cornerLen), strokeW)

                            // Top-Right corner bracket
                            drawLine(neonGreen, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                            drawLine(neonGreen, Offset(right, top), Offset(right, top + cornerLen), strokeW)

                            // Bottom-Left corner bracket
                            drawLine(neonGreen, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                            drawLine(neonGreen, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

                            // Bottom-Right corner bracket
                            drawLine(neonGreen, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                            drawLine(neonGreen, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

                            // Sliding Red Laser Sweep line
                            val laserY = top + (frameHeight * laserYOffset)
                            drawLine(
                                color = Color(0xFFEF4444),
                                start = Offset(left, laserY),
                                end = Offset(right, laserY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    // Scan Success Feedback overlay
                    if (scannedCodeFeedback != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF10B981).copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    text = "Code Identifié !",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = scannedCodeFeedback ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else if (scanMode == "camera" && cameraPermissionState.status.isGranted) {
                        Text(
                            text = "[ EN ATTENTE DE CODE-BARRES / QR ]",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            ),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }
                }

                // Saisie Manuelle Text Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("Saisir un code manuellement", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                scannedCodeFeedback = manualCode.trim()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Saisir")
                    }
                }

                // Simulation Picker list helper (Crucial for testing/demo)
                Text(
                    text = "Simulation rapide (Éléments enregistrés) :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrer la liste de simulation...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun élément disponible dans cette vue.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { scannedCodeFeedback = item.code },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${item.category} • Code: ${item.code}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Simuler le Scan",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
