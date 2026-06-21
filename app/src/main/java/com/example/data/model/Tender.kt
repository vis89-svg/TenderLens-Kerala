package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenders")
data class Tender(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val department: String,
    val district: String,
    val town: String,
    val estimatedCost: Double,
    val emd: Double,
    val fee: Double,
    val publishedDate: Long,
    val closingDate: Long,
    val category: String,
    val documents: String, // Comma-separated list of document names/urls
    val status: String = "Active", // "Active", "Updated", "Extended", "Cancelled"
    val isCancelled: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var isBookmarked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val daysRemaining: Int
        get() {
            val diff = closingDate - System.currentTimeMillis()
            return if (diff <= 0) 0 else (diff / (1000 * 60 * 60 * 24)).toInt()
        }
}
