package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserPreference
import com.example.ui.viewmodel.TenderViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: TenderViewModel,
    onOnboardingComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    val districtsList = listOf(
        "Kollam", "Thiruvananthapuram", "Alappuzha", "Ernakulam", "Idukki",
        "Kannur", "Kasaragod", "Kottayam", "Kozhikode", "Malappuram",
        "Palakkad", "Pathanamthitta", "Thrissur", "Wayanad"
    )

    val categoriesList = listOf(
        "Civil Works", "Electrical", "Road Works", "Solar", "Water Supply", "IT Services"
    )

    // User preference state local copies to configure
    var selectedDistricts by remember { mutableStateOf(setOf("Kollam", "Thiruvananthapuram", "Ernakulam")) }
    var selectedCategories by remember { mutableStateOf(categoriesList.toSet()) }
    var valuePreference by remember { mutableStateOf(500000.0) } // Default above 5 Lakhs
    var syncPeriodHours by remember { mutableStateOf(6) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("onboarding_screen"),
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        TextButton(
                            onClick = { step-- },
                            modifier = Modifier.testTag("onboarding_back")
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                val pref = UserPreference(
                                    selectedDistricts = selectedDistricts.joinToString(","),
                                    selectedCategories = selectedCategories.joinToString(","),
                                    valuePreference = valuePreference,
                                    monitoringFrequencyHours = syncPeriodHours
                                )
                                viewModel.completeOnboarding(pref)
                                onOnboardingComplete()
                            }
                        },
                        modifier = Modifier.testTag("onboarding_next"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (step == 3) "Start Monitoring Engine" else "Continue")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            // STEP PROGRESS indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 1..3) {
                    val isActive = i <= step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            when (step) {
                1 -> OnboardingWelcomeStep(
                    imageUrl = R.drawable.img_tender_onboarding,
                    onStartClick = { step = 2 }
                )
                2 -> OnboardingPreferencesStep(
                    districtsList = districtsList,
                    selectedDistricts = selectedDistricts,
                    onDistrictToggled = { dist ->
                        selectedDistricts = if (selectedDistricts.contains(dist)) {
                            selectedDistricts - dist
                        } else {
                            selectedDistricts + dist
                        }
                    },
                    categoriesList = categoriesList,
                    selectedCategories = selectedCategories,
                    onCategoryToggled = { cat ->
                        selectedCategories = if (selectedCategories.contains(cat)) {
                            selectedCategories - cat
                        } else {
                            selectedCategories + cat
                        }
                    }
                )
                3 -> OnboardingValueSyncStep(
                    valuePreference = valuePreference,
                    onValueSelected = { valuePreference = it },
                    syncPeriodHours = syncPeriodHours,
                    onSyncPeriodSelected = { syncPeriodHours = it }
                )
            }
        }
    }
}

@Composable
fun OnboardingWelcomeStep(imageUrl: Int, onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageUrl),
            contentDescription = "Onboarding background illustration",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TenderLens Kerala",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Continuous eTender monitoring for local smart contractors. Never check etenders.kerala.gov.in manually again.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                BulletFeatureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Automatic Snapshots & Alerts",
                    desc = "Continuously detects new tenders, deadline extensions, corrigendum uploads and cancellations in Kerala eTender Portal."
                )
                Spacer(modifier = Modifier.height(12.dp))
                BulletFeatureItem(
                    icon = Icons.Default.Business,
                    title = "Personalized Local Feed",
                    desc = "Get notified ONLY for opportunities in your selected Kerala districts and cost thresholds."
                )
            }
        }
    }
}

@Composable
fun OnboardingPreferencesStep(
    districtsList: List<String>,
    selectedDistricts: Set<String>,
    onDistrictToggled: (String) -> Unit,
    categoriesList: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggled: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Tell Us Your Business Scope",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "We will scan and alert you on tenders matching these parameters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Districts preference header
        Text(
            text = "Select Districts of Operation",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid for districts
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(districtsList) { dist ->
                val isSelected = selectedDistricts.contains(dist)
                FilterChip(
                    selected = isSelected,
                    onClick = { onDistrictToggled(dist) },
                    label = { Text(dist) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories preferences header
        Text(
            text = "Select Operational Trades",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid for categories
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoriesList) { cat ->
                val isSelected = selectedCategories.contains(cat)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryToggled(cat) },
                    label = { Text(cat) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingValueSyncStep(
    valuePreference: Double,
    onValueSelected: (Double) -> Unit,
    syncPeriodHours: Int,
    onSyncPeriodSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Value & Monitor Thresholds",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Set cost preferences and continuous snapshot checking frequency.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Minimum Tender Estimated Value",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val valuePreferences = listOf(
            0.0 to "Show All Values (No Minimum)",
            500000.0 to "Above ₹5 Lakhs",
            2500000.0 to "Above ₹25 Lakhs",
            5000000.0 to "Above ₹50 Lakhs",
            10000000.0 to "Above ₹1 Crore"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                valuePreferences.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = valuePreference == value,
                            onClick = { onValueSelected(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "eTender Portal SCAN Frequency",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Recommended: Every 6 Hours",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val frequencies = listOf(
            1 to "Every Hour",
            3 to "Every 3 Hours",
            6 to "Every 6 Hours (Speed & Battery)",
            12 to "Every 12 Hours",
            24 to "Daily Summary Sync"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                frequencies.forEach { (hours, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = syncPeriodHours == hours,
                            onClick = { onSyncPeriodSelected(hours) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun BulletFeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
