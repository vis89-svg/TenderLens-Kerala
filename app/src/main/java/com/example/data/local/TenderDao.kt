package com.example.data.local

import androidx.room.*
import com.example.data.model.Alert
import com.example.data.model.SavedSearch
import com.example.data.model.Tender
import com.example.data.model.UserPreference
import com.example.data.model.UserAccount
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TenderDao {
    @Query("SELECT * FROM tenders ORDER BY publishedDate DESC")
    fun getAllTenders(): Flow<List<Tender>>

    @Query("SELECT * FROM tenders WHERE id = :id LIMIT 1")
    fun getTenderByIdDirect(id: String): Tender?

    @Query("SELECT * FROM tenders WHERE id = :id LIMIT 1")
    fun getTenderById(id: String): Flow<Tender?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenders(tenders: List<Tender>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTender(tender: Tender)

    @Query("UPDATE tenders SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, isBookmarked: Boolean)

    @Update
    suspend fun updateTender(tender: Tender)

    @Query("SELECT * FROM tenders WHERE isBookmarked = 1 ORDER BY publishedDate DESC")
    fun getBookmarkedTenders(): Flow<List<Tender>>

    @Query("DELETE FROM tenders")
    suspend fun clearTenders()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<Alert>)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM alerts")
    suspend fun clearAlerts()
}

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getSavedSearchesForUser(userEmail: String): Flow<List<SavedSearch>>

    @Query("SELECT * FROM saved_searches ORDER BY timestamp DESC")
    fun getAllSavedSearches(): Flow<List<SavedSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedSearch(search: SavedSearch)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun deleteSavedSearch(id: Int)
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    fun getUserPreferenceFlow(id: Int): Flow<UserPreference?>

    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    suspend fun getUserPreference(id: Int): UserPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Update
    suspend fun updateUser(user: UserAccount)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM user_watchlist WHERE userEmail = :userEmail")
    fun getWatchlistForUser(userEmail: String): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItem)

    @Query("DELETE FROM user_watchlist WHERE userEmail = :userEmail AND tenderId = :tenderId")
    suspend fun removeWatchlistItem(userEmail: String, tenderId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM user_watchlist WHERE userEmail = :userEmail AND tenderId = :tenderId LIMIT 1)")
    fun isBookmarked(userEmail: String, tenderId: String): Flow<Boolean>
}
