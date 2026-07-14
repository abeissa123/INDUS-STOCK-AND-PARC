package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

data class ScannableItem(
    val code: String,
    val name: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    title: String = "Scanner d'Outils & Équipements",
    subtitle: String = "Pointez l'appareil photo vers le code-barres ou sélectionnez un outil ci-dessous",
    scannableItems: List<ScannableItem>,
    onDismiss: () -> Unit,
    onScanResult: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var manualCode by remember { mutableStateOf("") }
    
    // Laser Animation setup
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

    // Filtering items
    val filteredItems = remember(scannableItems, searchQuery) {
        if (searchQuery.isBlank()) {
            scannableItems.take(5) // Limit initial list length for cleaner UX
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
            delay(800) // visual feedback delay before closing
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

                // Main Viewfinder Simulation Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw a scanning target box overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        // Central framing parameters
                        val frameWidth = canvasWidth * 0.7f
                        val frameHeight = canvasHeight * 0.5f
                        val left = (canvasWidth - frameWidth) / 2
                        val top = (canvasHeight - frameHeight) / 2
                        val right = left + frameWidth
                        val bottom = top + frameHeight
                        
                        // Dark overlay outside the framing box
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            size = size
                        )
                        
                        // Draw clean white viewport rectangle with dashed strokes
                        drawRect(
                            color = Color.White.copy(alpha = 0.4f),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                        
                        // Green focus corner highlights
                        val cornerLen = 20.dp.toPx()
                        val strokeW = 4.dp.toPx()
                        val neonGreen = Color(0xFF10B981)
                        
                        // Top-Left corner
                        drawLine(neonGreen, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                        drawLine(neonGreen, Offset(left, top), Offset(left, top + cornerLen), strokeW)
                        
                        // Top-Right corner
                        drawLine(neonGreen, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                        drawLine(neonGreen, Offset(right, top), Offset(right, top + cornerLen), strokeW)
                        
                        // Bottom-Left corner
                        drawLine(neonGreen, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                        drawLine(neonGreen, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
                        
                        // Bottom-Right corner
                        drawLine(neonGreen, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                        drawLine(neonGreen, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

                        // Neon Red Laser Animation Sweep
                        val laserY = top + (frameHeight * laserYOffset)
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = Offset(left, laserY),
                            end = Offset(right, laserY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Success Feedback Indicator Overlays
                    if (scannedCodeFeedback != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF10B981).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "Scan réussi !",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = scannedCodeFeedback ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    } else {
                        // Viewfinder subtle text
                        Text(
                            text = "[ ALIGNER LE CODE-BARRES ]",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 2.sp
                            ),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }
                }

                // Manual Input Fallback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("Entrer un code manuellement", color = Color.Gray) },
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
                        Text("Valider")
                    }
                }

                // Interactive Quick Simulator Panel
                Text(
                    text = "Simulateur de Scan (Sélection d'éléments actifs) :",
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
                        Text("Aucun élément scannable dans cette vue.", color = Color.Gray)
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
