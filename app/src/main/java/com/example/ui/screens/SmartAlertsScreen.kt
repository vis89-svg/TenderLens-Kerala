package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alert
import com.example.ui.viewmodel.TenderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlertsScreen(
    viewModel: TenderViewModel,
    onBackClick: () -> Unit,
    onTenderClick: (String) -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()

    // Aggregate statistics of today's monitoring activities (Feature 16)
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayNewCount = alerts.count { it.timestamp >= todayStart && it.alertType == "NEW" }
    val todayExtendedCount = alerts.count { it.timestamp >= todayStart && it.alertType == "EXTENDED" }
    val todayCancelledCount = alerts.count { it.timestamp >= todayStart && it.alertType == "CANCELLED" }
    val todayCorrigendumCount = alerts.count { it.timestamp >= todayStart && it.alertType == "CORRIGENDUM" }

    val formattedTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portal Change History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Return")
                    }
                },
                actions = {
                    if (alerts.isNotEmpty()) {
                        IconButton(onClick = { viewModel.markAllAlertsAsRead() }) {
                            Icon(Icons.Filled.MarkEmailRead, contentDescription = "Mark all status read")
                        }
                        IconButton(onClick = { viewModel.clearAlerts() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Purge alerts logs history")
                        }
                    }
                }
            )
        },
        modifier = Modifier.testTag("alerts_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- DAILY SCANNING OVERVIEW DASHBOARD CARD (Feature 16) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Today's Portal Scanner Activity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryActivityBadge(title = "New", count = todayNewCount, color = MaterialTheme.colorScheme.primary)
                        SummaryActivityBadge(title = "Extended", count = todayExtendedCount, color = MaterialTheme.colorScheme.tertiary)
                        SummaryActivityBadge(title = "Files Mod", count = todayCorrigendumCount, color = MaterialTheme.colorScheme.secondary)
                        SummaryActivityBadge(title = "Cancel", count = todayCancelledCount, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ALERTS LOGS CHRONOLOGY LIST (Features 11 & 15)
            if (alerts.isEmpty()) {
                EmptyStatePlaceholder(
                    title = "Scan history is currently clear",
                    subtitle = "All portal detections will be recorded chronologically here. Use 'Scan Kerala eTenders' on the Dashboard to simulate a continuous portal analysis check.",
                    icon = Icons.Default.CircleNotifications
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { alert ->
                        AlertLogItem(
                            alert = alert,
                            formattedTime = formattedTime.format(Date(alert.timestamp)),
                            onClick = {
                                viewModel.markAlertAsRead(alert.id)
                                onTenderClick(alert.tenderId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryActivityBadge(
    title: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(66.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlertLogItem(
    alert: Alert,
    formattedTime: String,
    onClick: () -> Unit
) {
    val (icon, color, typeLabel) = when (alert.alertType) {
        "NEW" -> Triple(Icons.Default.FiberNew, MaterialTheme.colorScheme.primary, "NEW OPPORTUNITY")
        "EXTENDED" -> Triple(Icons.Default.DateRange, MaterialTheme.colorScheme.tertiary, "DEADLINE CHANGED")
        "CANCELLED" -> Triple(Icons.Default.Cancel, MaterialTheme.colorScheme.error, "TENDER REMOVED")
        else -> Triple(Icons.Default.Description, MaterialTheme.colorScheme.secondary, "DOCUMENT CORRIGENDUM")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (alert.isRead) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else color.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(34.dp)
                    .background(color.copy(alpha = 0.08f), CircleShape)
                    .padding(6.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alert.tenderTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
