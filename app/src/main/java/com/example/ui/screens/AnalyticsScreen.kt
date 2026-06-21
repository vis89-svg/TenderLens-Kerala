package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tender
import com.example.ui.viewmodel.TenderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: TenderViewModel,
    onBackClick: () -> Unit
) {
    val rawTenders by viewModel.rawTenders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Return")
                    }
                }
            )
        },
        modifier = Modifier.testTag("analytics_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                "Tender Distribution Insights",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Market metrics summarizing active eTenders across various segments of Kerala.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. CHART BY DISTRICT
            val districtGroups = rawTenders.groupBy { it.district }
                .map { (district, list) -> district to list.size }
                .sortedByDescending { it.second }

            AnalyticsSectionCard(title = "Top Active Districts (Counts)") {
                districtGroups.take(6).forEach { (district, count) ->
                    val maxRatio = districtGroups.firstOrNull()?.second ?: 1
                    val fillRatio = count.toFloat() / maxRatio
                    ProgressBarStatRow(label = district, count = count, ratio = fillRatio, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. CHART BY TRADE CATEGORY
            val categoryGroups = rawTenders.groupBy { it.category }
                .map { (cat, list) -> cat to list.size }
                .sortedByDescending { it.second }

            AnalyticsSectionCard(title = "Trade Categories Breakdown") {
                categoryGroups.forEach { (category, count) ->
                    val maxRatio = categoryGroups.firstOrNull()?.second ?: 1
                    val fillRatio = count.toFloat() / maxRatio
                    ProgressBarStatRow(label = category, count = count, ratio = fillRatio, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. CHART BY ESTIMATED COST RANGE
            val budgetRanges = listOf(
                "₹0 - ₹10 Lakhs" to rawTenders.count { it.estimatedCost <= 1000000.0 },
                "₹10 - ₹50 Lakhs" to rawTenders.count { it.estimatedCost > 1000000.0 && it.estimatedCost <= 5000000.0 },
                "₹50 Lakhs - ₹1 Crore" to rawTenders.count { it.estimatedCost > 5000000.0 && it.estimatedCost <= 10000000.0 },
                "₹1 Crore+" to rawTenders.count { it.estimatedCost > 10000000.0 }
            )

            AnalyticsSectionCard(title = "Distribution by Tender Capital Size") {
                budgetRanges.forEach { (label, count) ->
                    val maxRatio = budgetRanges.maxOf { it.second }.coerceAtLeast(1)
                    val fillRatio = count.toFloat() / maxRatio
                    ProgressBarStatRow(label = label, count = count, ratio = fillRatio, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. CHART BY PUBLISHING DEPARTMENTS
            val deptGroups = rawTenders.groupBy { it.department.split(" (").first() }
                .map { (dept, list) -> dept to list.size }
                .sortedByDescending { it.second }

            AnalyticsSectionCard(title = "Department Market Shares") {
                deptGroups.take(5).forEach { (dept, count) ->
                    val maxRatio = deptGroups.firstOrNull()?.second ?: 1
                    val fillRatio = count.toFloat() / maxRatio
                    ProgressBarStatRow(label = dept, count = count, ratio = fillRatio, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AnalyticsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ProgressBarStatRow(
    label: String,
    count: Int,
    ratio: Float,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text("$count tenders", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceIn(0.01f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
