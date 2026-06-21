package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_searches")
data class SavedSearch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String = "",
    val name: String,
    val query: String = "",
    val district: String? = null,
    val category: String? = null,
    val minCost: Double? = null,
    val department: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
