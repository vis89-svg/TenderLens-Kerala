package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Alert
import com.example.data.model.SavedSearch
import com.example.data.model.Tender
import com.example.data.model.UserPreference
import com.example.data.model.UserAccount
import com.example.data.model.WatchlistItem
import com.example.data.remote.GeminiClient
import com.example.data.repository.TenderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TenderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TenderRepository
    private val sharedPrefs = application.getSharedPreferences("tender_lens_prefs", Context.MODE_PRIVATE)

    private val _currentUserEmail = MutableStateFlow<String?>(sharedPrefs.getString("logged_in_email", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TenderRepository(
            database.tenderDao(),
            database.alertDao(),
            database.savedSearchDao(),
            database.userPreferenceDao(),
            database.userAccountDao(),
            database.watchlistDao()
        )

        viewModelScope.launch {
            repository.initializeSeedData()
        }
    }

    // Direct flows from database
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userPreference: StateFlow<UserPreference?> = _currentUserEmail.flatMapLatest { email ->
        if (email.isNullOrBlank()) {
            repository.getUserPreferenceFlowForEmail("")
        } else {
            repository.getUserPreferenceFlowForEmail(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rawTenders: StateFlow<List<Tender>> = repository.allTenders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userWatchlist: StateFlow<List<WatchlistItem>> = _currentUserEmail.flatMapLatest { email ->
        if (email.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            repository.getWatchlistForUser(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTendersWithBookmarks: StateFlow<List<Tender>> = combine(rawTenders, userWatchlist) { tenders, watchlist ->
        val watchlistTenders = watchlist.map { it.tenderId }.toSet()
        tenders.map { tender ->
            tender.copy(isBookmarked = watchlistTenders.contains(tender.id))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<Alert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val savedSearches: StateFlow<List<SavedSearch>> = _currentUserEmail.flatMapLatest { email ->
        if (email.isNullOrBlank()) {
            repository.getSavedSearchesForUser("")
        } else {
            repository.getSavedSearchesForUser(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filtering States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedDistrict = MutableStateFlow("All")
    val selectedDistrict = _selectedDistrict.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedDepartment = MutableStateFlow("All")
    val selectedDepartment = _selectedDepartment.asStateFlow()

    private val _selectedValueFilter = MutableStateFlow("All")
    val selectedValueFilter = _selectedValueFilter.asStateFlow()

    // --- Nearby Settings ---
    private val _nearbyRadius = MutableStateFlow(25.0f) // in km
    val nearbyRadius = _nearbyRadius.asStateFlow()

    private val _mockUserLat = MutableStateFlow(8.8932) // Default is Kollam coordinate
    val mockUserLat = _mockUserLat.asStateFlow()

    private val _mockUserLng = MutableStateFlow(76.6141)
    val mockUserLng = _mockUserLng.asStateFlow()

    // --- Monitoring & Interaction states ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _latestScanResultCount = MutableStateFlow<Int?>(null)
    val latestScanResultCount = _latestScanResultCount.asStateFlow()

    val syncHealth = repository.syncHealth

    // --- AI Assistant / Gemini States ---
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _selectedTenderId = MutableStateFlow<String?>(null)
    val selectedTenderId = _selectedTenderId.asStateFlow()

    val selectedTender: StateFlow<Tender?> = combine(rawTenders, _selectedTenderId) { tenders, id ->
        tenders.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Helper data class to bundle search parameters
    data class SearchFilters(
        val query: String,
        val district: String,
        val category: String,
        val department: String,
        val valueFilter: String
    )

    private val filterCohort1 = combine(_searchQuery, _selectedDistrict, _selectedCategory) { query, district, category ->
        Triple(query, district, category)
    }

    private val filterCohort2 = combine(_selectedDepartment, _selectedValueFilter) { dept, value ->
        Pair(dept, value)
    }

    private val searchFiltersFlow = combine(filterCohort1, filterCohort2) { cohort1, cohort2 ->
        SearchFilters(
            query = cohort1.first,
            district = cohort1.second,
            category = cohort1.third,
            department = cohort2.first,
            valueFilter = cohort2.second
        )
    }

    // Filtered Tenders Feed for personalized dashboard / feed
    val filteredTenders: StateFlow<List<Tender>> = combine(
        allTendersWithBookmarks,
        searchFiltersFlow
    ) { tenders, filters ->
        tenders.filter { tender ->
            val query = filters.query.trim()
            val matchesQuery = query.isBlank() ||
                    tender.title.contains(query, ignoreCase = true) ||
                    tender.description.contains(query, ignoreCase = true) ||
                    tender.town.contains(query, ignoreCase = true) ||
                    tender.district.contains(query, ignoreCase = true) ||
                    tender.id.contains(query, ignoreCase = true) ||
                    (query.contains("trivandrum", ignoreCase = true) && tender.district.contains("Thiruvananthapuram", ignoreCase = true)) ||
                    (query.contains("thiruvananthapuram", ignoreCase = true) && tender.district.contains("Thiruvananthapuram", ignoreCase = true)) ||
                    (query.contains("tvm", ignoreCase = true) && tender.district.contains("Thiruvananthapuram", ignoreCase = true))

            val matchesDistrict = filters.district == "All" ||
                    tender.district.equals(filters.district, ignoreCase = true) ||
                    (filters.district.equals("Thiruvananthapuram", ignoreCase = true) && tender.district.equals("Trivandrum", ignoreCase = true)) ||
                    (filters.district.equals("Trivandrum", ignoreCase = true) && tender.district.equals("Thiruvananthapuram", ignoreCase = true))
            val matchesCategory = filters.category == "All" || tender.category.equals(filters.category, ignoreCase = true)
            val matchesDepartment = filters.department == "All" || tender.department.equals(filters.department, ignoreCase = true)

            val matchesValue = when (filters.valueFilter) {
                "₹0 - ₹1 Lakh" -> tender.estimatedCost <= 100000.0
                "₹1 - ₹10 Lakhs" -> tender.estimatedCost > 100000.0 && tender.estimatedCost <= 1000000.0
                "₹10 - ₹50 Lakhs" -> tender.estimatedCost > 1000000.0 && tender.estimatedCost <= 5000000.0
                "₹50 Lakhs - ₹1 Crore" -> tender.estimatedCost > 5000000.0 && tender.estimatedCost <= 10000000.0
                "₹1 Crore+" -> tender.estimatedCost > 10000000.0
                else -> true // "All"
            }

            matchesQuery && matchesDistrict && matchesCategory && matchesDepartment && matchesValue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personalized Tender Feed matches user preferences exactly (Location, Category, Value Preference)
    val personalizedFeed: StateFlow<List<Tender>> = combine(
        allTendersWithBookmarks,
        userPreference
    ) { tenders, pref ->
        if (pref == null) return@combine tenders

        val selectedDistricts = pref.getDistrictsList()
        val selectedCategories = pref.getCategoriesList()

        tenders.filter { tender ->
            val matchesPrefDistrict = selectedDistricts.isEmpty() || selectedDistricts.any { it.trim().equals(tender.district, true) }
            val matchesPrefCategory = selectedCategories.isEmpty() || selectedCategories.any { it.trim().equals(tender.category, true) }
            val matchesPrefValue = tender.estimatedCost >= pref.valuePreference

            matchesPrefDistrict && matchesPrefCategory && matchesPrefValue && !tender.isCancelled
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Nearby Tenders calculated based on spherical distance
    val nearbyTenders: StateFlow<List<Tender>> = combine(
        allTendersWithBookmarks,
        _nearbyRadius,
        _mockUserLat,
        _mockUserLng
    ) { tenders, radius, userLat, userLng ->
        tenders.filter { tender ->
            if (tender.isCancelled) return@filter false
            val distance = calculateDistanceInKm(userLat, userLng, tender.latitude, tender.longitude)
            distance <= radius
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Action Methods ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDistrictChanged(district: String) {
        _selectedDistrict.value = district
    }

    fun onCategoryChanged(category: String) {
        _selectedCategory.value = category
    }

    fun onDepartmentChanged(department: String) {
        _selectedDepartment.value = department
    }

    fun onValueFilterChanged(valueFilter: String) {
        _selectedValueFilter.value = valueFilter
    }

    fun onNearbyRadiusChanged(radius: Float) {
        _nearbyRadius.value = radius
    }

    fun setMockUserLocation(lat: Double, lng: Double) {
        _mockUserLat.value = lat
        _mockUserLng.value = lng
    }

    fun selectTender(id: String?) {
        _selectedTenderId.value = id
        _aiResponse.value = null // Reset assistant prompt
    }

    fun toggleBookmark(id: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            if (email == null) {
                repository.toggleBookmark(id, isBookmarked)
            } else {
                if (isBookmarked) {
                    repository.addWatchlistItem(email, id)
                } else {
                    repository.removeWatchlistItem(email, id)
                }
            }
        }
    }

    fun applySavedSearch(search: SavedSearch) {
        _selectedDistrict.value = search.district ?: "All"
        _selectedCategory.value = search.category ?: "All"
        _selectedDepartment.value = search.department ?: "All"
        _searchQuery.value = search.query
        search.minCost?.let {
            if (it <= 100000.0) _selectedValueFilter.value = "₹0 - ₹1 Lakh"
            else if (it <= 1000000.0) _selectedValueFilter.value = "₹1 - ₹10 Lakhs"
            else if (it <= 5000000.0) _selectedValueFilter.value = "₹10 - ₹50 Lakhs"
            else if (it <= 10000000.0) _selectedValueFilter.value = "₹50 Lakhs - ₹1 Crore"
            else _selectedValueFilter.value = "₹1 Crore+"
        }
    }

    fun saveCurrentFiltersAsSearch(name: String) {
        viewModelScope.launch {
            val minCost = when (_selectedValueFilter.value) {
                "₹0 - ₹1 Lakh" -> 0.0
                "₹1 - ₹10 Lakhs" -> 100000.0
                "₹10 - ₹50 Lakhs" -> 1000000.0
                "₹50 Lakhs - ₹1 Crore" -> 5000000.0
                "₹1 Crore+" -> 10000000.0
                else -> null
            }
            val search = SavedSearch(
                userEmail = _currentUserEmail.value ?: "",
                name = name,
                query = _searchQuery.value,
                district = if (_selectedDistrict.value == "All") null else _selectedDistrict.value,
                category = if (_selectedCategory.value == "All") null else _selectedCategory.value,
                department = if (_selectedDepartment.value == "All") null else _selectedDepartment.value,
                minCost = minCost
            )
            val email = _currentUserEmail.value
            if (email == null) {
                repository.addSavedSearch(search)
            } else {
                repository.addSavedSearchForUser(email, search)
            }
        }
    }

    fun removeSavedSearch(id: Int) {
        viewModelScope.launch {
            repository.removeSavedSearch(id)
        }
    }

    fun completeOnboarding(pref: UserPreference) {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            if (email == null) {
                repository.savePreferences(pref.copy(onboardingCompleted = true))
            } else {
                repository.savePreferencesForEmail(email, pref.copy(onboardingCompleted = true))
                val account = repository.getUserByEmail(email)
                if (account != null) {
                    repository.updateUser(account.copy(onboardingCompleted = true))
                }
            }
        }
    }

    fun updatePreferences(pref: UserPreference) {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            if (email == null) {
                repository.savePreferences(pref)
            } else {
                repository.savePreferencesForEmail(email, pref)
            }
        }
    }

    // --- Authentication Actions ---
    fun signUp(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                onError("Email and password cannot be empty")
                return@launch
            }
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onError("An account with this email already exists")
                return@launch
            }
            val newUser = UserAccount(email = email, passwordHash = password, onboardingCompleted = false)
            repository.registerUser(newUser)

            // Save login session
            sharedPrefs.edit().putString("logged_in_email", email).apply()
            _currentUserEmail.value = email
            onSuccess()
        }
    }

    fun logIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                onError("Email and password cannot be empty")
                return@launch
            }
            val existing = repository.getUserByEmail(email)
            if (existing == null) {
                onError("No account found with this email")
                return@launch
            }
            if (existing.passwordHash != password) {
                onError("Incorrect password")
                return@launch
            }

            // Save login session
            sharedPrefs.edit().putString("logged_in_email", email).apply()
            _currentUserEmail.value = email
            onSuccess()
        }
    }

    fun logOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            sharedPrefs.edit().remove("logged_in_email").apply()
            _currentUserEmail.value = null
            onComplete()
        }
    }

    fun markAlertAsRead(id: Int) {
        viewModelScope.launch {
            repository.markAlertAsRead(id)
        }
    }

    fun markAllAlertsAsRead() {
        viewModelScope.launch {
            repository.markAllAlertsAsRead()
        }
    }

    fun clearAlerts() {
        viewModelScope.launch {
            repository.clearAlerts()
        }
    }

    // TRIGGER PORTAL SCAN WORKFLOW
    fun triggerPortalScan(context: Context) {
        if (_isScanning.value) return
        _isScanning.value = true
        _latestScanResultCount.value = null

        viewModelScope.launch {
            try {
                val alertsCount = repository.runCrawlerSynchronization(context)
                _latestScanResultCount.value = alertsCount
            } catch (e: Exception) {
                _latestScanResultCount.value = 0
            } finally {
                _isScanning.value = false
            }
        }
    }

    // CALL COGNITIVE ASSISTANT (GEMINI) TO ANALYZE SELECTED TENDER
    fun askAiAssistantAboutTender(tender: Tender, question: String) {
        _isAiLoading.value = true
        _aiResponse.value = null

        viewModelScope.launch {
            val systemPrompt = """
                You are high-tech "TenderLens Kerala Assistant". Help the user analyze government tenders.
                Be concise, structured, professional, and practical. Offer strategic suggestions, warnings,
                estimated expenses, and check timelines or EMD clauses clearly.
                Provide formatting like bold fields, custom lists, and a brief final advice.
            """.trimIndent()

            val formattedCost = when {
                tender.estimatedCost >= 10000000 -> String.format("%.2f Crore", tender.estimatedCost / 10000000)
                tender.estimatedCost >= 100000 -> String.format("%.2f Lakh", tender.estimatedCost / 100000)
                else -> String.format("%.0f", tender.estimatedCost)
            }

            val prompt = """
                Analyze this Tender specifications and reply to user's question.
                
                TENDER DETAILS:
                Tender ID: ${tender.id}
                Title: ${tender.title}
                Department: ${tender.department}
                District: ${tender.district}, Town: ${tender.town}
                Estimated Value: ₹$formattedCost
                EMD Cost: ₹${tender.emd}
                Tender Document Fee: ₹${tender.fee}
                Specification Summary: ${tender.description}
                
                USER'S QUESTION:
                $question
            """.trimIndent()

            val response = GeminiClient.generateText(prompt, systemPrompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    // Spherical distance utility
    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
