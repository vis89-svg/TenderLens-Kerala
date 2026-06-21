package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val selectedDistricts: String = "", // Comma-separated or empty (meaning All)
    val selectedCategories: String = "", // Comma-separated or empty (meaning All)
    val valuePreference: Double = 0.0, // Minimum tender value in Rupees (e.g. 500000.0)
    val monitoringFrequencyHours: Int = 6 // Default 6 hours
) {
    fun getDistrictsList(): List<String> {
        return if (selectedDistricts.isBlank()) emptyList() else selectedDistricts.split(",")
    }

    fun getCategoriesList(): List<String> {
        return if (selectedCategories.isBlank()) emptyList() else selectedCategories.split(",")
    }
}
