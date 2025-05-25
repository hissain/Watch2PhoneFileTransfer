package com.hissain.android.demo.datastorelib

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heartRates: List<HeartRate>)

    @Query("SELECT * FROM heart_rate WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    fun getHeartRateData(from: Instant, to: Instant): Flow<List<HeartRate>>

    @Query("DELETE FROM heart_rate WHERE timestamp < :cutoff")
    suspend fun deleteOldData(cutoff: Instant)
}

@Dao
interface RespirationRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(respirationRates: List<RespirationRate>)

    @Query("SELECT * FROM respiration_rate WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    fun getRespirationRateData(from: Instant, to: Instant): Flow<List<RespirationRate>>

    @Query("DELETE FROM respiration_rate WHERE timestamp < :cutoff")
    suspend fun deleteOldData(cutoff: Instant)
}

@Dao
interface EdaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(edaData: List<Eda>)

    @Query("SELECT * FROM eda WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    fun getEdaData(from: Instant, to: Instant): Flow<List<Eda>>

    @Query("DELETE FROM eda WHERE timestamp < :cutoff")
    suspend fun deleteOldData(cutoff: Instant)
}

@Dao
interface TemperatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(temperatures: List<Temperature>)

    @Query("SELECT * FROM temperature WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    fun getTemperatureData(from: Instant, to: Instant): Flow<List<Temperature>>

    @Query("DELETE FROM temperature WHERE timestamp < :cutoff")
    suspend fun deleteOldData(cutoff: Instant)
}