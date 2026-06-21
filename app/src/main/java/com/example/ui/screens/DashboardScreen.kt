package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavedSearch
import com.example.data.model.Tender
import com.example.data.model.UserPreference
import com.example.ui.viewmodel.TenderViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TenderViewModel,
    onTenderClick: (String) -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel collected states
    val userPref by viewModel.userPreference.collectAsState()
    val rawTenders by viewModel.rawTenders.collectAsState()
    val allTendersWithBookmarks by viewModel.allTendersWithBookmarks.collectAsState()
    val filteredTenders by viewModel.filteredTenders.collectAsState()
    val personalizedFeed by viewModel.personalizedFeed.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val savedSearches by viewModel.savedSearches.collectAsState()

    // Filter controls
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDistrict by viewModel.selectedDistrict.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedValueFilter by viewModel.selectedValueFilter.collectAsState()

    // Scan stats
    val isScanning by viewModel.isScanning.collectAsState()
    val latestScanResultCount by viewModel.latestScanResultCount.collectAsState()

    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var showDistrictFilterDialog by remember { mutableStateOf(false) }
    var showValueFilterDialog by remember { mutableStateOf(false) }
    var showSaveSearchDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var newSavedSearchName by remember { mutableStateOf("") }

    var currentTabSelected by remember { mutableStateOf(0) } // 0 = Personalized Feed, 1 = Search & Filter, 2 = Watchlist

    val bookmarkedTenders = allTendersWithBookmarks.filter { it.isBookmarked }

    // Aggregate statistics
    val closingTodayCount = rawTenders.count { !it.isCancelled && isDueToday(it.closingDate) }
    val closingThisWeekCount = rawTenders.count { !it.isCancelled && it.daysRemaining in 1..7 }
    val totalActiveTendersCount = rawTenders.count { !it.isCancelled }
    val unreadAlertsCount = alerts.count { !it.isRead }

    val districtsList = listOf(
        "All", "Kollam", "Thiruvananthapuram", "Alappuzha", "Ernakulam", "Idukki",
        "Kannur", "Kasaragod", "Kottayam", "Kozhikode", "Malappuram",
        "Palakkad", "Pathanamthitta", "Thrissur", "Wayanad"
    )

    val categoriesList = listOf(
        "All", "Civil Works", "Electrical", "Road Works", "Solar", "Water Supply", "IT Services"
    )

    val valueFiltersList = listOf(
        "All", "₹0 - ₹1 Lakh", "₹1 - ₹10 Lakhs", "₹10 - ₹50 Lakhs", "₹50 Lakhs - ₹1 Crore", "₹1 Crore+"
    )

    Scaffold(
        modifier = Modifier.testTag("dashboard_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.triggerPortalScan(context)
                },
                icon = {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = "Manual sync scan")
                    }
                },
                text = { Text(if (isScanning) "Scanning Portal..." else "Scan Kerala eTenders") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("scan_trigger_button")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // HEADER BANNER WITH COGNITIVE OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "Technological surveillance portal representation background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TenderLens Kerala",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Portal intelligence & active monitoring.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Navigation to auxiliary utilities
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val currentUserEmail by viewModel.currentUserEmail.collectAsState()

                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                                .testTag("dashboard_profile_button")
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "User profile and session settings", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onNavigateToAlerts,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadAlertsCount > 0) {
                                        Badge { Text(unreadAlertsCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Alerts history drawer", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        IconButton(
                            onClick = onNavigateToNearby,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Nearby tender discovery map", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onNavigateToAnalytics,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Filled.PieChart, contentDescription = "Tenders stats charts", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // DYNAMIC SCAN NOTIFICATION BANNER
            AnimatedVisibility(
                visible = latestScanResultCount != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                latestScanResultCount?.let { count ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (count > 0) Icons.Default.NewReleases else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (count > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (count > 0) "Scan Completed: $count eTender Updates Detected!" else "eTender Scan: Portal is fully synchronized.",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (count > 0) "New updates log generated in Change History." else "Your snapshot database matches the latest active listings.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.triggerPortalScan(context) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Remonitir scan")
                            }
                        }
                    }
                }
            }

            // BENTO GRID SUMMARY SECTION (Features 10 & 16 with Bento Grid theme styling)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left main Bento Card (weight = 2) - active status summary
                Card(
                    modifier = Modifier
                        .weight(2f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F3F3)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0xFFB2DEDE))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Today's Activity".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF006A6A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = String.format("%02d", totalActiveTendersCount),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        
                        val prefDesc = remember(userPref) {
                            val districts = userPref?.getDistrictsList() ?: emptyList()
                            val categories = userPref?.getCategoriesList() ?: emptyList()
                            val distStr = if (districts.isEmpty()) "Kerala" else districts.first()
                            val catStr = if (categories.isEmpty()) "civil & general works" else categories.joinToString(", ").lowercase()
                            "Relevant to your $catStr filters in $distStr."
                        }
                        
                        Text(
                            text = prefDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right stacked column Bento Cards (weight = 1) - quick metrics
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Bento box: Closing Today (Red/Rose accent)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE7E9)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFF9C3C7))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%02d", closingTodayCount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFBA1A1A)
                            )
                            Text(
                                text = "Closing Today".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF410002)
                            )
                        }
                    }

                    // Bottom Bento box: Closing This Week / Extended (Yellow/Amber accent)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4D9)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE082))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%02d", closingThisWeekCount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF7B5900)
                            )
                            Text(
                                text = "Due 7 Days".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF251A00)
                            )
                        }
                    }
                }
            }

            // TAB BAR (Personalized, Search, Watchlist)
            TabRow(
                selectedTabIndex = currentTabSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTabSelected == 0,
                    onClick = { currentTabSelected = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Personalized Feed")
                        }
                    },
                    modifier = Modifier.testTag("tab_personalized")
                )
                Tab(
                    selected = currentTabSelected == 1,
                    onClick = { currentTabSelected = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Tenders")
                        }
                    },
                    modifier = Modifier.testTag("tab_filtered")
                )
                Tab(
                    selected = currentTabSelected == 2,
                    onClick = { currentTabSelected = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watchlist (${bookmarkedTenders.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_watchlist")
                )
            }

            when (currentTabSelected) {
                0 -> {
                    // TAB 0: PERSONALIZED FEED ACCORDING TO CONTRACT PREFERENCES
                    Column(modifier = Modifier.weight(1f)) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = "Preference settings summaries", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "My Intelligence Filters",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        userPref?.let { pref ->
                                            Text(
                                                text = "${pref.getDistrictsList().size} Dist. • ${pref.getCategoriesList().size} Trades • Value >= ₹${formatPreferenceValue(pref.valuePreference)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { viewModel.completeOnboarding(userPref ?: UserPreference()) }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modify", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (personalizedFeed.isEmpty()) {
                            EmptyStatePlaceholder(
                                title = "No personalized tenders found",
                                subtitle = "No portal entries currently match your location, trade categories, or value criteria. Run a manual sync scan to look for new updates, or expand your scope filters.",
                                icon = Icons.Default.FilterListOff
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(personalizedFeed) { tender ->
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
                1 -> {
                    // TAB 1: ALL ACTIVE TENDERS SEARCH & FILTER ENGINE
                    Column(modifier = Modifier.weight(1f)) {
                        // Search text box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search location, title, tender ID...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                } else {
                                    IconButton(onClick = { showSaveSearchDialog = true }) {
                                        Icon(Icons.Default.Save, contentDescription = "Save selection as quick queries")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("smart_search_field"),
                            maxLines = 1,
                            singleLine = true
                        )

                        // SAVED SEARCHES QUICK ROW (Feature 13)
                        if (savedSearches.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    Text(
                                        "Saved Queries:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(savedSearches) { search ->
                                    InputChip(
                                        selected = false,
                                        onClick = { viewModel.applySavedSearch(search) },
                                        label = { Text(search.name) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove saved search",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeSavedSearch(search.id) }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // ADVANCED FILTER ACCORDIONS HEADER (Features 3, 6, 8)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElevatedFilterButton(
                                prefix = "District",
                                value = selectedDistrict,
                                onClick = { showDistrictFilterDialog = true },
                                modifier = Modifier.weight(1.0f)
                            )
                            ElevatedFilterButton(
                                prefix = "Trade",
                                value = selectedCategory,
                                onClick = { showCategoryFilterDialog = true },
                                modifier = Modifier.weight(1.0f)
                            )
                            ElevatedFilterButton(
                                prefix = "Estimate",
                                value = selectedValueFilter,
                                onClick = { showValueFilterDialog = true },
                                modifier = Modifier.weight(1.0f)
                            )
                        }

                        if (filteredTenders.isEmpty()) {
                            EmptyStatePlaceholder(
                                title = "No matching tenders",
                                subtitle = "Try executing a simpler keyword search or relaxing filter options like District or Cost ranges.",
                                icon = Icons.Default.HistoryToggleOff
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredTenders) { tender ->
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
                2 -> {
                    // TAB 2: WATCHLIST PINNED TENDERS (Feature 12)
                    Column(modifier = Modifier.weight(1f)) {
                        if (bookmarkedTenders.isEmpty()) {
                            EmptyStatePlaceholder(
                                title = "Your watchlists look empty",
                                subtitle = "Pin active tenders of interest by clicking the bookmark icon. We will continuously track updates, changes, and deadline shifts on watchlisted items.",
                                icon = Icons.Default.BookmarkBorder
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(bookmarkedTenders) { tender ->
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
        }
    }

    // --- DISTRICT SELECTOR DIALOG ---
    if (showDistrictFilterDialog) {
        AlertDialog(
            onDismissRequest = { showDistrictFilterDialog = false },
            title = { Text("Filter by Kerala District") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                    items(districtsList) { dist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onDistrictChanged(dist)
                                    showDistrictFilterDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDistrict == dist,
                                onClick = {
                                    viewModel.onDistrictChanged(dist)
                                    showDistrictFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(dist, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDistrictFilterDialog = false }) { Text("Close") }
            }
        )
    }

    // --- USER PROFILE & LOGOUT DIALOG ---
    if (showProfileDialog) {
        val currentUserEmail by viewModel.currentUserEmail.collectAsState()
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF006A6A),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Contractor Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Account Session",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = currentUserEmail ?: "Guest Contractor",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tracks individual watchlist bookmarks, custom Trade Sectors preference and saved queries under this safe personal profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logOut {
                            showProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("dashboard_logout_button")
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- CATEGORY SELECTOR DIALOG ---
    if (showCategoryFilterDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryFilterDialog = false },
            title = { Text("Filter by Trade Sector") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                    items(categoriesList) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onCategoryChanged(cat)
                                    showCategoryFilterDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == cat,
                                onClick = {
                                    viewModel.onCategoryChanged(cat)
                                    showCategoryFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryFilterDialog = false }) { Text("Close") }
            }
        )
    }

    // --- VALUE SELECTOR DIALOG ---
    if (showValueFilterDialog) {
        AlertDialog(
            onDismissRequest = { showValueFilterDialog = false },
            title = { Text("Filter by Estimated Expense") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                    items(valueFiltersList) { valueRange ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onValueFilterChanged(valueRange)
                                    showValueFilterDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedValueFilter == valueRange,
                                onClick = {
                                    viewModel.onValueFilterChanged(valueRange)
                                    showValueFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(valueRange, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showValueFilterDialog = false }) { Text("Close") }
            }
        )
    }

    // --- SAVE SEARCH POPUP ---
    if (showSaveSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSearchDialog = false },
            title = { Text("Save Current Filter Set") },
            text = {
                Column {
                    Text(
                        "Store these parameters as a quick Saved Search for one-tap dashboard access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newSavedSearchName,
                        onValueChange = { newSavedSearchName = it },
                        label = { Text("Search Title Name (e.g., Kollam Civil Works)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSavedSearchName.isNotBlank()) {
                            viewModel.saveCurrentFiltersAsSearch(newSavedSearchName)
                            newSavedSearchName = ""
                            showSaveSearchDialog = false
                        }
                    }
                ) {
                    Text("Save Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveSearchDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier.height(78.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    lineHeight = 24.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun ElevatedFilterButton(
    prefix: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (value == "All") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value == "All") "$prefix: All" else value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TenderListItem(
    tender: Tender,
    onTenderClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTenderClick),
        colors = CardDefaults.cardColors(
            containerColor = when (tender.status) {
                "Cancelled" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                "Extended" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.04f)
            }
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            when (tender.status) {
                "Cancelled" -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                "Extended" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title status and Bookmark action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge Icon
                    Icon(
                        imageVector = when (tender.category) {
                            "Road Works" -> Icons.Default.Map
                            "Water Supply" -> Icons.Default.Opacity
                            "Solar" -> Icons.Default.LightMode
                            "Electrical" -> Icons.Default.Bolt
                            "IT Services" -> Icons.Default.Computer
                            else -> Icons.Default.Build
                        },
                        contentDescription = "Category",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .padding(4.dp)
                    )

                    Column {
                        Text(
                            text = tender.department.split(" (").first(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${tender.town}, ${tender.district}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bookmark trigger
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (tender.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Pin eTender",
                        tint = if (tender.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main tender title
            Text(
                text = tender.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cost structure information row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ESTIMATED COST",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₹${formatValue(tender.estimatedCost)}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Status Badge Indicator
                    StatusBadge(status = tender.status)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (tender.isCancelled) "Cancelled" else "Closing: ${tender.daysRemaining} days left",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (tender.isCancelled) MaterialTheme.colorScheme.error else if (tender.daysRemaining <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "Cancelled" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "CANCELLED")
        "Extended" -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "DEADLINE EXTENDED")
        "Updated" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "UPDATED")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "ACTIVE")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
            color = textColor
        )
    }
}

@Composable
fun EmptyStatePlaceholder(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Utility formatting methods
fun formatValue(value: Double): String {
    return when {
        value >= 10000000 -> String.format("%.2f Cr", value / 10000000)
        value >= 100000 -> String.format("%.2f L", value / 100000)
        else -> String.format("%.0f", value)
    }
}

fun formatPreferenceValue(value: Double): String {
    return when {
         value >= 10000000 -> String.format("%.0f Crore", value / 10000000)
         value >= 100000 -> String.format("%.0f Lakhs", value / 100000)
         else -> String.format("%.0f", value)
    }
}

fun isDueToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = today + 24 * 60 * 60 * 1000L
    return timestamp in today until tomorrow
}
