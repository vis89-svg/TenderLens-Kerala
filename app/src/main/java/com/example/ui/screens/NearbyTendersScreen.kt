package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tender
import com.example.ui.viewmodel.TenderViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyTendersScreen(
    viewModel: TenderViewModel,
    onBackClick: () -> Unit,
    onTenderClick: (String) -> Unit
) {
    val nearbyTenders by viewModel.nearbyTenders.collectAsState()
    val nearbyRadius by viewModel.nearbyRadius.collectAsState()
    val mockUserLat by viewModel.mockUserLat.collectAsState()
    val mockUserLng by viewModel.mockUserLng.collectAsState()

    val scope = rememberCoroutineScope()

    var selectedPositionIndex by remember { mutableStateOf(0) } // 0 = Kollam, 1 = Thiruvananthapuram, 2 = Ernakulam

    val mockLocations = listOf(
        Triple("Kollam (Chavara Core)", 8.8932, 76.6141),
        Triple("Thiruvananthapuram City", 8.5241, 76.9366),
        Triple("Ernakulam (Aluva Division)", 9.9816, 76.2999)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Tender Radar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Return")
                    }
                }
            )
        },
        modifier = Modifier.testTag("nearby_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Mock Location Quick Anchors Row
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select GPS Radar Center Base:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mockLocations.forEachIndexed { index, item ->
                        val isSelected = selectedPositionIndex == index
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPositionIndex = index
                                viewModel.setMockUserLocation(item.second, item.third)
                            },
                            label = { Text(item.first, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTOR RADAR SURVEILLANCE MAP CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Draw dynamic radar circle lines using Canvas!
                Canvas(
                    modifier = Modifier
                        .size(170.dp)
                        .padding(8.dp)
                ) {
                    val centerPt = Offset(size.width / 2f, size.height / 2f)

                    // Draw concentric radar lines
                    drawCircle(
                        color = RadarColor.PrimaryContainerLines,
                        radius = size.width / 2f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = RadarColor.PrimaryContainerLines,
                        radius = size.width / 3.2f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = RadarColor.PrimaryContainerLines,
                        radius = size.width / 6f,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw axes lines
                    drawLine(
                        color = RadarColor.PrimaryContainerLines,
                        start = Offset(centerPt.x, 0f),
                        end = Offset(centerPt.x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = RadarColor.PrimaryContainerLines,
                        start = Offset(0f, centerPt.y),
                        end = Offset(size.width, centerPt.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw a scanning radial line decoration
                    val angleRad = Math.toRadians((System.currentTimeMillis() / 8 % 360).toDouble())
                    val endPt = Offset(
                        (centerPt.x + size.width / 2f * cos(angleRad)).toFloat(),
                        (centerPt.y + size.height / 2f * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = RadarColor.ScanningPulse,
                        start = centerPt,
                        end = endPt,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Draw focus point
                    drawCircle(
                        color = RadarColor.CenterLocationPin,
                        radius = 6.dp.toPx()
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scanning Area Radius Threshold: ${nearbyRadius.toInt()} km",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // DYNAMIC RADIUS CONFIGURATION SLIDER
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                Slider(
                    value = nearbyRadius,
                    onValueChange = { viewModel.onNearbyRadiusChanged(it) },
                    valueRange = 10.0f..100.0f,
                    steps = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10 km", style = MaterialTheme.typography.bodySmall)
                    Text("25 km", style = MaterialTheme.typography.bodySmall)
                    Text("50 km", style = MaterialTheme.typography.bodySmall)
                    Text("100 km", style = MaterialTheme.typography.bodySmall)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // MAP SCAN ITEMS LIST HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Tenders Identified (${nearbyTenders.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (nearbyTenders.isEmpty()) {
                EmptyStatePlaceholder(
                    title = "No tenders within this circle",
                    subtitle = "Expand the scanning radius slider or select a different GPS center to identify opportunities nearby.",
                    icon = Icons.Default.MyLocation
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(nearbyTenders) { tender ->
                        TenderListItem(
                            tender = tender,
                            onTenderClick = { onTenderClick(tender.id) },
                            onBookmarkClick = { viewModel.toggleBookmark(tender.id, !tender.isBookmarked) }
                        )
                    }
                }
            }
        }
    }
}

// Custom local theme color configurations safe from Compose Theme dependencies
private object RadarColor {
    val PrimaryContainerLines = Color(0xFF009688).copy(alpha = 0.35f)
    val ScanningPulse = Color(0xFF009688).copy(alpha = 0.85f)
    val CenterLocationPin = Color(0xFFFF5722)
}
