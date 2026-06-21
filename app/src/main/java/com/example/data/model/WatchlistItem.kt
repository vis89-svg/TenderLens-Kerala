package com.example.data.model

import androidx.room.Entity

@Entity(tableName = "user_watchlist", primaryKeys = ["userEmail", "tenderId"])
data class WatchlistItem(
    val userEmail: String,
    val tenderId: String
)
