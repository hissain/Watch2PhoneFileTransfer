package com.hissain.android.demo.datastorelib

enum class SensorType {
    HEART_RATE,
    RESPIRATION_RATE,
    EDA,
    TEMPERATURE
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object InProgress : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String, val exception: Throwable?) : SyncStatus()
}

data class SensorDataQuery(
    val sensorType: SensorType,
    val fromTimestamp: java.time.Instant,
    val toTimestamp: java.time.Instant
)