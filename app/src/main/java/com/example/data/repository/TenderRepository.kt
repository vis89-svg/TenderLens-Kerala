package com.example.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AlertDao
import com.example.data.local.SavedSearchDao
import com.example.data.local.TenderDao
import com.example.data.local.UserPreferenceDao
import com.example.data.local.UserAccountDao
import com.example.data.local.WatchlistDao
import com.example.data.model.Alert
import com.example.data.model.SavedSearch
import com.example.data.model.Tender
import com.example.data.model.UserPreference
import com.example.data.model.UserAccount
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Random

class TenderRepository(
    private val tenderDao: TenderDao,
    private val alertDao: AlertDao,
    private val savedSearchDao: SavedSearchDao,
    private val userPreferenceDao: UserPreferenceDao,
    private val userAccountDao: UserAccountDao,
    private val watchlistDao: WatchlistDao
) {
    val allTenders: Flow<List<Tender>> = tenderDao.getAllTenders()
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()
    val savedSearches: Flow<List<SavedSearch>> = savedSearchDao.getAllSavedSearches()
    val userPreference: Flow<UserPreference?> = userPreferenceDao.getUserPreferenceFlow(1)

    private val random = Random()

    companion object {
        private const val CHANNEL_ID = "tender_lens_alerts"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    suspend fun initializeSeedData() = withContext(Dispatchers.IO) {
        // Only initialize default guest if preferences don't exist yet
        val existingPref = userPreferenceDao.getUserPreference(1)
        if (existingPref == null) {
            val defaultPref = UserPreference(
                id = 1,
                onboardingCompleted = false,
                selectedDistricts = "Kollam,Thiruvananthapuram,Ernakulam",
                selectedCategories = "Civil Works,Electrical,Road Works",
                valuePreference = 500000.0, // Above ₹5 Lakhs
                monitoringFrequencyHours = 6
            )
            userPreferenceDao.insertPreference(defaultPref)

            // Seed 15 initial classic Kerala tenders
            val seedTenders = getSeedTendersList()
            tenderDao.insertTenders(seedTenders)

            // Add standard saved searches
            savedSearchDao.insertSavedSearch(SavedSearch(
                userEmail = "",
                name = "Kollam Road Works",
                district = "Kollam",
                category = "Road Works"
            ))
            savedSearchDao.insertSavedSearch(SavedSearch(
                userEmail = "",
                name = "PWD Civil Above 25L",
                department = "Public Works Department (PWD)",
                category = "Civil Works",
                minCost = 2500000.0
            ))

            // Add a few initial historical alerts to make dashboard lively
            val initialAlerts = listOf(
                Alert(
                    tenderId = "KER-2026-PWD-001",
                    tenderTitle = "Reconstruction of Bridge at Karunagappally Junction",
                    alertType = "NEW",
                    description = "New Civil Tender published in Kollam by PWD.",
                    timestamp = System.currentTimeMillis() - 7200000,
                    district = "Kollam",
                    category = "Civil Works",
                    estimatedCost = 4500000.0,
                    department = "Public Works Department (PWD)"
                ),
                Alert(
                    tenderId = "KER-2026-KWA-002",
                    tenderTitle = "Replacement of Water Mains in Varkala Town",
                    alertType = "EXTENDED",
                    description = "Closing date extended to 30 June 2026.",
                    timestamp = System.currentTimeMillis() - 14400000,
                    district = "Thiruvananthapuram",
                    category = "Water Supply",
                    estimatedCost = 1800000.0,
                    department = "Kerala Water Authority (KWA)"
                )
            )
            alertDao.insertAlerts(initialAlerts)
        }
    }

    // --- Dynamic / User Scoped Flow Functions ---
    fun getUserPreferenceFlowForEmail(email: String): Flow<UserPreference?> {
        return userPreferenceDao.getUserPreferenceFlow(email.hashCode())
    }

    suspend fun getUserPreferenceForEmail(email: String): UserPreference? = withContext(Dispatchers.IO) {
        userPreferenceDao.getUserPreference(email.hashCode())
    }

    suspend fun savePreferencesForEmail(email: String, pref: UserPreference) = withContext(Dispatchers.IO) {
        userPreferenceDao.insertPreference(pref.copy(id = email.hashCode()))
    }

    fun getSavedSearchesForUser(email: String): Flow<List<SavedSearch>> {
        return savedSearchDao.getSavedSearchesForUser(email)
    }

    suspend fun addSavedSearchForUser(email: String, search: SavedSearch) = withContext(Dispatchers.IO) {
        savedSearchDao.insertSavedSearch(search.copy(userEmail = email))
    }

    // --- Authentication Core Methods ---
    suspend fun getUserByEmail(email: String): UserAccount? = withContext(Dispatchers.IO) {
        userAccountDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: UserAccount) = withContext(Dispatchers.IO) {
        userAccountDao.insertUser(user)
        // Also initialize preferences for the user immediately with clean defaults
        val defaultPref = UserPreference(
            id = user.email.hashCode(),
            onboardingCompleted = false,
            selectedDistricts = "Kollam,Thiruvananthapuram,Ernakulam",
            selectedCategories = "Civil Works,Electrical,Road Works",
            valuePreference = 0.0,
            monitoringFrequencyHours = 6
        )
        userPreferenceDao.insertPreference(defaultPref)
    }

    suspend fun updateUser(user: UserAccount) = withContext(Dispatchers.IO) {
        userAccountDao.updateUser(user)
    }

    // --- User Watchlist (Bookmarks) Scoped Methods ---
    fun getWatchlistForUser(email: String): Flow<List<WatchlistItem>> {
        return watchlistDao.getWatchlistForUser(email)
    }

    suspend fun addWatchlistItem(email: String, tenderId: String) = withContext(Dispatchers.IO) {
        watchlistDao.insertWatchlistItem(WatchlistItem(userEmail = email, tenderId = tenderId))
    }

    suspend fun removeWatchlistItem(email: String, tenderId: String) = withContext(Dispatchers.IO) {
        watchlistDao.removeWatchlistItem(userEmail = email, tenderId = tenderId)
    }

    fun isBookmarkedForUser(email: String, tenderId: String): Flow<Boolean> {
        return watchlistDao.isBookmarked(email, tenderId)
    }

    suspend fun savePreferences(pref: UserPreference) = withContext(Dispatchers.IO) {
        userPreferenceDao.insertPreference(pref)
    }

    suspend fun toggleBookmark(tenderId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        tenderDao.updateBookmark(tenderId, isBookmarked)
    }

    suspend fun addSavedSearch(search: SavedSearch) = withContext(Dispatchers.IO) {
        savedSearchDao.insertSavedSearch(search)
    }

    suspend fun removeSavedSearch(id: Int) = withContext(Dispatchers.IO) {
        savedSearchDao.deleteSavedSearch(id)
    }

    suspend fun markAlertAsRead(id: Int) = withContext(Dispatchers.IO) {
        alertDao.markAsRead(id)
    }

    suspend fun markAllAlertsAsRead() = withContext(Dispatchers.IO) {
        alertDao.markAllAsRead()
    }

    suspend fun clearAlerts() = withContext(Dispatchers.IO) {
        alertDao.clearAlerts()
    }

    // SIMULATED SCAN ENGINE COMPARING LOCAL SNAPSHOT WITH THE NEW STATE
    suspend fun runMonitoringScan(context: Context): Int = withContext(Dispatchers.IO) {
        val currentTenders = allTenders.first().associateBy { it.id }
        if (currentTenders.isEmpty()) return@withContext 0

        val alertsGenerated = mutableListOf<Alert>()
        val updatedTenders = mutableListOf<Tender>()

        val userPref = userPreferenceDao.getUserPreference(1) ?: UserPreference()

        // 1. Randomly decide which events to simulate
        val actionType = random.nextInt(5) // 0 = New Tender, 1 = Extend Closing Date, 2 = Value Update, 3 = Cancel, 4 = Clean/No Scan alert

        when (actionType) {
            0 -> {
                // Simulate publishing a NEW Tender
                val newTender = generateRandomTender()
                tenderDao.insertTender(newTender)
                updatedTenders.add(newTender)

                val alert = Alert(
                    tenderId = newTender.id,
                    tenderTitle = newTender.title,
                    alertType = "NEW",
                    description = "New ${newTender.category} tender published in ${newTender.district} by ${newTender.department}. Value: ₹${formatValue(newTender.estimatedCost)}",
                    timestamp = System.currentTimeMillis(),
                    district = newTender.district,
                    category = newTender.category,
                    estimatedCost = newTender.estimatedCost,
                    department = newTender.department
                )
                alertsGenerated.add(alert)

                // Trigger Android System Notification
                if (isTenderMatchingPreferences(newTender, userPref)) {
                    triggerAndroidNotification(
                        context,
                        "Smart Kerala eTender Alert",
                        "New PWD/Govt Tender: ${newTender.title} in ${newTender.district}, Value: ₹${formatValue(newTender.estimatedCost)}"
                    )
                }
            }
            1 -> {
                // Simulate EXTENDING an existing tender's deadline
                val randomTender = currentTenders.values.randomOrNull()
                if (randomTender != null && !randomTender.isCancelled) {
                    val extendedDate = randomTender.closingDate + (5 * 24 * 60 * 60 * 1000L) // Add 5 days
                    val modifiedTender = randomTender.copy(
                        closingDate = extendedDate,
                        status = "Extended",
                        lastUpdated = System.currentTimeMillis()
                    )
                    tenderDao.insertTender(modifiedTender)
                    updatedTenders.add(modifiedTender)

                    val alert = Alert(
                        tenderId = randomTender.id,
                        tenderTitle = randomTender.title,
                        alertType = "EXTENDED",
                        description = "Closing date extended to ${formatDate(extendedDate)}.",
                        timestamp = System.currentTimeMillis(),
                        district = randomTender.district,
                        category = randomTender.category,
                        estimatedCost = randomTender.estimatedCost,
                        department = randomTender.department
                    )
                    alertsGenerated.add(alert)

                    triggerAndroidNotification(
                        context,
                        "Deadline Extended",
                        "Tender closing date moved dynamically: ${randomTender.title}"
                    )
                }
            }
            2 -> {
                // Simulate general tender update (e.g., corrigendum or minor corrigendum doc uploaded)
                val randomTender = currentTenders.values.randomOrNull()
                if (randomTender != null && !randomTender.isCancelled) {
                    val docList = if (randomTender.documents.isBlank()) "Corrigendum_1.pdf" else "${randomTender.documents},Corrigendum_Doc_V1.pdf"
                    val modifiedTender = randomTender.copy(
                        documents = docList,
                        status = "Updated",
                        lastUpdated = System.currentTimeMillis()
                    )
                    tenderDao.insertTender(modifiedTender)
                    updatedTenders.add(modifiedTender)

                    val alert = Alert(
                        tenderId = randomTender.id,
                        tenderTitle = randomTender.title,
                        alertType = "CORRIGENDUM",
                        description = "New corrigendum document uploaded: Corrigendum_Doc_V1.pdf.",
                        timestamp = System.currentTimeMillis(),
                        district = randomTender.district,
                        category = randomTender.category,
                        estimatedCost = randomTender.estimatedCost,
                        department = randomTender.department
                    )
                    alertsGenerated.add(alert)

                    triggerAndroidNotification(
                        context,
                        "Document Corrigendum Added",
                        "New files are available for: ${randomTender.title}"
                    )
                }
            }
            3 -> {
                // Simulate cancellation
                val randomTender = currentTenders.values.filter { !it.isCancelled }.randomOrNull()
                if (randomTender != null) {
                    val modifiedTender = randomTender.copy(
                        isCancelled = true,
                        status = "Cancelled",
                        lastUpdated = System.currentTimeMillis()
                    )
                    tenderDao.insertTender(modifiedTender)
                    updatedTenders.add(modifiedTender)

                    val alert = Alert(
                        tenderId = randomTender.id,
                        tenderTitle = randomTender.title,
                        alertType = "CANCELLED",
                        description = "Tender has been officially CANCELLED by ${randomTender.department}.",
                        timestamp = System.currentTimeMillis(),
                        district = randomTender.district,
                        category = randomTender.category,
                        estimatedCost = randomTender.estimatedCost,
                        department = randomTender.department
                    )
                    alertsGenerated.add(alert)

                    triggerAndroidNotification(
                        context,
                        "Tender Cancelled",
                        "Tender removed from active listings: ${randomTender.title}"
                    )
                }
            }
            else -> {
                // No changes detected on this automated polling hour
                Log.d("ScanEngine", "No eTender updates detected on this portal scan.")
            }
        }

        if (alertsGenerated.isNotEmpty()) {
            alertDao.insertAlerts(alertsGenerated)
        }

        return@withContext alertsGenerated.size
    }

    private fun isTenderMatchingPreferences(tender: Tender, pref: UserPreference): Boolean {
        val distList = pref.getDistrictsList()
        val catList = pref.getCategoriesList()

        val matchesDistrict = distList.isEmpty() || distList.contains(tender.district)
        val matchesCategory = catList.isEmpty() || catList.contains(tender.category)
        val matchesValue = tender.estimatedCost >= pref.valuePreference

        return matchesDistrict && matchesCategory && matchesValue
    }

    private fun formatValue(value: Double): String {
        return when {
            value >= 10000000 -> String.format("%.2f Crore", value / 10000000)
            value >= 100000 -> String.format("%.2f Lakh", value / 100000)
            else -> String.format("%.0f", value)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun triggerAndroidNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tender alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about new tenders and extensions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Using default system drawable as safe icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + random.nextInt(100), notification)
    }

    private fun generateRandomTender(): Tender {
        val uid = "KER-2026-GEN-${random.nextInt(100000)}"
        val dist = listOf(
            "Kollam", "Thiruvananthapuram", "Alappuzha", "Ernakulam", "Idukki",
            "Kannur", "Kasaragod", "Kottayam", "Kozhikode", "Malappuram",
            "Palakkad", "Pathanamthitta", "Thrissur", "Wayanad"
        ).random()

        val dept = listOf(
            "Public Works Department (PWD)",
            "Kerala Water Authority (KWA)",
            "Kerala State Electricity Board (KSEB)",
            "Local Self Government Department (LSGD)",
            "Forest Department",
            "Health Department"
        ).random()

        val cat = listOf(
            "Civil Works", "Electrical", "Road Works", "Solar", "Water Supply", "IT Services"
        ).random()

        val (title, town, cost) = when (cat) {
            "Civil Works" -> Triple(
                listOf(
                    "Construction of New District Office Annex Building",
                    "Renovation of Panchayath Community Hall",
                    "Construction of Retaining Wall and Fencing for Govt School"
                ).random() + " at " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (3000000..45000000).random().toDouble()
            )
            "Road Works" -> Triple(
                listOf(
                    "Tarring and Drainage Improvement of Bypass Road",
                    "Interlocking Concrete Block Paving of Market Road",
                    "Berm Protection and Repair of Primary Highway Section"
                ).random() + " in " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (1200000..18000000).random().toDouble()
            )
            "Electrical" -> Triple(
                listOf(
                    "HT Line Extension and Installation of 100KVA Transformer",
                    "Wiring and Panel Upgrades for Medical College Wing",
                    "Street Lighting Project using Smart LED Fixtures"
                ).random() + " in " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (500000..12000000).random().toDouble()
            )
            "Solar" -> Triple(
                listOf(
                    "Grid Connected Rooftop Solar Power Plant Installation",
                    "Solar High-Mast Lighting Systems in Public Squares",
                    "Installation of Solar Grid Inverters at Sub-stations"
                ).random() + " near " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (800000..9500000).random().toDouble()
            )
            "Water Supply" -> Triple(
                listOf(
                    "WSS Pipe Laying and Retrofitting of Distribution Grid",
                    "Overhead Water Tank Construction and Pump Room Assembly",
                    "Supply of Flowmeters and HDPE Pipeline Layout"
                ).random() + " at " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (1500000..32000000).random().toDouble()
            )
            else -> Triple(
                listOf(
                    "Development of e-Governance Portal and Helpdesk System",
                    "CCTV Camera Network Installation and Control Center Setup",
                    "Hardware Procurement and Networking of Taluk Labs"
                ).random() + " in " + getRandomTownForDistrict(dist),
                getRandomTownForDistrict(dist),
                (400000..7500000).random().toDouble()
            )
        }

        val emd = cost * 0.01
        val fee = when {
            cost < 1000000 -> 1500.0
            cost < 10000000 -> 2500.0
            else -> 7500.0
        }

        val published = System.currentTimeMillis() - (random.nextInt(3) * 24 * 60 * 60 * 1000L)
        val closing = System.currentTimeMillis() + ((5 + random.nextInt(15)) * 24 * 60 * 60 * 1000L)

        val (lat, lng) = getCoordinatesForDistrict(dist)

        return Tender(
            id = uid,
            title = title,
            description = "This tender is officially initiated by $dept for executing $title according to the enclosed drawing charts, schedules, terms and instructions. Bids must be submitted strictly via the online Kerala eTender utility prior to deadlines.",
            department = dept,
            district = dist,
            town = town,
            estimatedCost = cost,
            emd = emd,
            fee = fee,
            publishedDate = published,
            closingDate = closing,
            category = cat,
            documents = "Tender_Schedule_V1.pdf,General_Instructions.pdf",
            latitude = lat + (random.nextDouble() - 0.5) * 0.05,
            longitude = lng + (random.nextDouble() - 0.5) * 0.05
        )
    }

    private fun getRandomTownForDistrict(district: String): String {
        return when (district) {
            "Kollam" -> listOf("Karunagappally", "Punalur", "Chathannoor", "Anchal", "Kottarakkara", "Chavara").random()
            "Thiruvananthapuram" -> listOf("Neyyattinkara", "Varkala", "Nedumangad", "Kovalam", "Attingal", "Vizhinjam").random()
            "Alappuzha" -> listOf("Kayamkulam", "Cherthala", "Ambalappuzha", "Haripad", "Mavelikkara", "Chengannur").random()
            "Ernakulam" -> listOf("Aluva", "Thoppumpady", "Kochi", "Muvattupuzha", "Perumbavoor", "Kalamassery", "Angamaly").random()
            "Idukki" -> listOf("Munnar", "Thodupuzha", "Adimali", "Nedumkandam", "Kumily", "Kattappana").random()
            "Kozhikode" -> listOf("Vatakara", "Quilandy", "Feroke", "Ramanattukara", "Koduvally").random()
            "Wayanad" -> listOf("Sultan Bathery", "Kalpetta", "Mananthavady", "Vythiri", "Meppadi").random()
            "Kottayam" -> listOf("Changanassery", "Pala", "Kanjirappally", "Vaikom", "Ettumanoor").random()
            "Thrissur" -> listOf("Chalakudy", "Guruvayur", "Irinjalakuda", "Kunnamkulam", "Kodungallur").random()
            "Palakkad" -> listOf("Ottapalam", "Shoranur", "Chittur", "Mannarkkad", "Alathur").random()
            "Malappuram" -> listOf("Manjeri", "Tirur", "Ponnani", "Kottakkal", "Perinthalmanna", "Nilambur").random()
            "Pathanamthitta" -> listOf("Adoor", "Thiruvalla", "Konni", "Ranni", "Pandalam").random()
            "Kannur" -> listOf("Thalassery", "Taliparamba", "Payyanur", "Mattannur", "Iritty").random()
            "Kasaragod" -> listOf("Kanhangad", "Nileshwaram", "Uppala", "Trikaripur").random()
            else -> "Central Town"
        }
    }

    private fun getCoordinatesForDistrict(district: String): Pair<Double, Double> {
        return when (district) {
            "Thiruvananthapuram" -> Pair(8.5241, 76.9366)
            "Kollam" -> Pair(8.8932, 76.6141)
            "Alappuzha" -> Pair(9.4981, 76.3388)
            "Pathanamthitta" -> Pair(9.2648, 76.7870)
            "Kottayam" -> Pair(9.5916, 76.5222)
            "Idukki" -> Pair(9.8504, 77.1065)
            "Ernakulam" -> Pair(9.9816, 76.2999)
            "Thrissur" -> Pair(10.5276, 76.2144)
            "Palakkad" -> Pair(10.7867, 76.6548)
            "Malappuram" -> Pair(11.0734, 76.0711)
            "Kozhikode" -> Pair(11.2588, 75.7804)
            "Wayanad" -> Pair(11.6854, 76.1320)
            "Kannur" -> Pair(11.8745, 75.3704)
            "Kasaragod" -> Pair(12.4996, 74.9869)
            else -> Pair(10.0, 76.0)
        }
    }

    private fun getSeedTendersList(): List<Tender> {
        val list = mutableListOf<Tender>()
        val districts = listOf(
            "Kollam", "Thiruvananthapuram", "Ernakulam", "Wayanad", "Alappuzha",
            "Pathanamthitta", "Kottayam", "Idukki", "Thrissur", "Palakkad", "Kozhikode"
        )
        val categories = listOf("Civil Works", "Electrical", "Road Works", "Solar", "Water Supply", "IT Services")

        // Seed 15 detailed initial tenders
        val seedRaw = listOf(
            Triple("KER-2026-PWD-001", "PWD", "Road Repairs and Bituminous Tarring under Panchayat Scheme in Karunagappally"),
            Triple("KER-2026-KWA-002", "KWA", "Laying of Distribution Pipelines and Flowmeter Fitting in Varkala"),
            Triple("KER-2026-KSEB-003", "KSEB", "Supply, Installation and Commissioning of 15KW Floating Solar Modules at Aluva Plant"),
            Triple("KER-2026-LSGD-004", "LSGD", "Renovation of Community Health Center and Parking Shed Yard at Adoor"),
            Triple("KER-2026-PWD-005", "PWD", "Construction of Multi-Storey Court Complex Annex and Utility Section in Ernakulam"),
            Triple("KER-2026-KWA-006", "KWA", "Implementation of Jal Jeevan Mission Rural Drinking Water Connections in Vythiri"),
            Triple("KER-2026-FOREST-007", "Forest", "Erection of Solar-Powered Hanging Fencing along Forest Border in Sultan Bathery"),
            Triple("KER-2026-KSEB-008", "KSEB", "Conversion of Over Head LT Lines to Underground Cabling in Fort Kochi Area"),
            Triple("KER-2026-HEALTH-009", "Health", "Procurement, Installation and Networking of Medical Laboratory Equipment at Neyyattinkara"),
            Triple("KER-2026-LSGD-010", "LSGD", "Comprehensive Digitalization of Panchayath Records and Setup of Citizen Kiosks at Punalur"),
            Triple("KER-2026-PWD-011", "PWD", "Reconstruction and Safety Guard Installation for Minor Bridge across Stream at Munnar"),
            Triple("KER-2026-KWA-012", "KWA", "Sinking of Borewell, Valve Room Construction and Submersible Pump Provision in Cherthala"),
            Triple("KER-2026-KSEB-013", "KSEB", "Erection of 11KV Pole Lines and HT Insulation Protection Work in Kalpetta Division"),
            Triple("KER-2026-LSGD-014", "LSGD", "Waste Segregation Yard Shed Civil Works and Yard Interlocking at Mavelikkara"),
            Triple("KER-2026-PWD-015", "PWD", "Pre-monsoon Desilting of Core Main Canals and Drainage Channels in Kottayam")
        )

        var days = 1
        for (item in seedRaw) {
            val dName = when (item.second) {
                "PWD" -> "Public Works Department (PWD)"
                "KWA" -> "Kerala Water Authority (KWA)"
                "KSEB" -> "Kerala State Electricity Board (KSEB)"
                "LSGD" -> "Local Self Government Department (LSGD)"
                "Forest" -> "Forest Department"
                else -> "Health Department"
            }

            val category = when {
                item.third.contains("Road", true) || item.third.contains("Tarring", true) -> "Road Works"
                item.third.contains("Water", true) || item.third.contains("Pipeline", true) || item.third.contains("Borewell", true) -> "Water Supply"
                item.third.contains("Solar", true) -> "Solar"
                item.third.contains("Cable", true) || item.third.contains("Electrical", true) || item.third.contains("Lines", true) -> "Electrical"
                item.third.contains("Digital", true) || item.third.contains("e-Gov", true) -> "IT Services"
                else -> "Civil Works"
            }

            val district = when {
                item.third.contains("Karunagappally", true) || item.third.contains("Punalur", true) -> "Kollam"
                item.third.contains("Varkala", true) || item.third.contains("Neyyattinkara", true) -> "Thiruvananthapuram"
                item.third.contains("Ernakulam", true) || item.third.contains("Kochi", true) || item.third.contains("Aluva", true) -> "Ernakulam"
                item.third.contains("Vythiri", true) || item.third.contains("Sultan Bathery", true) || item.third.contains("Kalpetta", true) -> "Wayanad"
                item.third.contains("Cherthala", true) || item.third.contains("Mavelikkara", true) -> "Alappuzha"
                item.third.contains("Adoor", true) -> "Pathanamthitta"
                item.third.contains("Munnar", true) -> "Idukki"
                else -> "Kottayam"
            }

            val town = when {
                item.third.contains("Karunagappally", true) -> "Karunagappally"
                item.third.contains("Punalur", true) -> "Punalur"
                item.third.contains("Varkala", true) -> "Varkala"
                item.third.contains("Neyyattinkara", true) -> "Neyyattinkara"
                item.third.contains("Ernakulam", true) -> "Ernakulam"
                item.third.contains("Fort Kochi", true) -> "Fort Kochi"
                item.third.contains("Aluva", true) -> "Aluva"
                item.third.contains("Vythiri", true) -> "Vythiri"
                item.third.contains("Sultan Bathery", true) -> "Sultan Bathery"
                item.third.contains("Kalpetta", true) -> "Kalpetta"
                item.third.contains("Cherthala", true) -> "Cherthala"
                item.third.contains("Mavelikkara", true) -> "Mavelikkara"
                item.third.contains("Adoor", true) -> "Adoor"
                item.third.contains("Munnar", true) -> "Munnar"
                else -> "Kottayam"
            }

            val cost = when (category) {
                "Civil Works" -> (2500000..35000000).random().toDouble()
                "Road Works" -> (1500000..20000000).random().toDouble()
                "Water Supply" -> (1000000..18000000).random().toDouble()
                "Solar" -> (800000..9000000).random().toDouble()
                "Electrical" -> (600000..8000000).random().toDouble()
                else -> (400000..5000000).random().toDouble()
            }

            val emd = cost * 0.015
            val fee = if (cost < 5000000) 2000.0 else 5500.0
            val (lat, lng) = getCoordinatesForDistrict(district)

            list.add(
                Tender(
                    id = item.first,
                    title = item.third,
                    description = "Comprehensive project executing modern safety designs and requirements as specified by the engineering guidelines of $dName. All credentials, tender documents, schedule of rates, and guidelines can be downloaded in full. Offline execution details are managed through our local snapshot registry.",
                    department = dName,
                    district = district,
                    town = town,
                    estimatedCost = cost,
                    emd = emd,
                    fee = fee,
                    publishedDate = System.currentTimeMillis() - (days * 12 * 60 * 60 * 1000L),
                    closingDate = System.currentTimeMillis() + (days * 4 * 24 * 60 * 60 * 1000L),
                    category = category,
                    documents = "Tender_Drawing_Sheet.pdf,Standard_Item_Rate_Schedule.pdf",
                    latitude = lat + (random.nextDouble() - 0.5) * 0.04,
                    longitude = lng + (random.nextDouble() - 0.5) * 0.04
                )
            )
            days++
        }
        return list
    }
}
