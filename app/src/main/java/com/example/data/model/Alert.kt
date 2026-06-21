package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenderId: String,
    val tenderTitle: String,
    val alertType: String, // "NEW", "UPDATED", "EXTENDED", "CANCELLED", "CORRIGENDUM"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val district: String = "",
    val category: String = "",
    val estimatedCost: Double = 0.0,
    val department: String = ""
)
