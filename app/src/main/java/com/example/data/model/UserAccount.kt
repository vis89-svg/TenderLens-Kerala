package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val onboardingCompleted: Boolean = false
)
