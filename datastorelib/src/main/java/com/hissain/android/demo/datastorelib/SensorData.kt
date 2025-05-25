package com.hissain.android.demo.datastorelib

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "heart_rate")
data class HeartRate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val heartRate: Float,
    val confidenceScore: Float
)

@Entity(tableName = "respiration_rate")
data class RespirationRate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val respirationRate: Float,
    val ibi: Float // Inter-beat interval
)

@Entity(tableName = "eda")
data class Eda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val edaValue: Float
)

@Entity(tableName = "temperature")
data class Temperature(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val tempValue: Float
)