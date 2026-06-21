package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Alert
import com.example.data.model.SavedSearch
import com.example.data.model.Tender
import com.example.data.model.UserPreference
import com.example.data.model.UserAccount
import com.example.data.model.WatchlistItem

@Database(
    entities = [Tender::class, Alert::class, SavedSearch::class, UserPreference::class, UserAccount::class, WatchlistItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenderDao(): TenderDao
    abstract fun alertDao(): AlertDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tender_lens_kerala_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
