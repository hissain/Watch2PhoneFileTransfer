package com.hissain.android.demo.datastorelib

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class DataStoreLib private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DataStoreLib? = null

        fun getInstance(context: Context): DataStoreLib {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreLib(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val database = Room.databaseBuilder(
        context,
        SensorDatabase::class.java,
        "sensor_database"
    ).build()

    private val _syncStatus = MutableLiveData<SyncStatus>(SyncStatus.Idle)
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun startSync(zipFilePath: String) = withContext(Dispatchers.IO) {
        try {
            _syncStatus.postValue(SyncStatus.InProgress)

            val tempDir = File(context.cacheDir, "sensor_temp")
            tempDir.mkdirs()

            // Unzip the file
            unzipFile(zipFilePath, tempDir.absolutePath)

            // Process each CSV file
            val heartRateFile = File(tempDir, "heart_rate.csv")
            val respirationFile = File(tempDir, "respiration_rate.csv")
            val edaFile = File(tempDir, "eda.csv")
            val temperatureFile = File(tempDir, "temperature.csv")

            if (heartRateFile.exists()) {
                parseAndStoreHeartRate(heartRateFile)
            }

            if (respirationFile.exists()) {
                parseAndStoreRespiration(respirationFile)
            }

            if (edaFile.exists()) {
                parseAndStoreEda(edaFile)
            }

            if (temperatureFile.exists()) {
                parseAndStoreTemperature(temperatureFile)
            }

            // Cleanup
            tempDir.deleteRecursively()

            _syncStatus.postValue(SyncStatus.Success("Data synchronized successfully"))

        } catch (e: Exception) {
            _syncStatus.postValue(SyncStatus.Error("Sync failed: ${e.message}", e))
        }
    }

    private suspend fun unzipFile(zipFilePath: String, destDirectory: String) {
        val buffer = ByteArray(1024)
        val zipInputStream = ZipInputStream(FileInputStream(zipFilePath))

        zipInputStream.use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(destDirectory, zipEntry.name)

                // Security check for path traversal
                if (!newFile.canonicalPath.startsWith(File(destDirectory).canonicalPath)) {
                    throw SecurityException("Path traversal attempt detected")
                }

                newFile.parentFile?.mkdirs()

                if (!zipEntry.isDirectory) {
                    FileOutputStream(newFile).use { fos ->
                        var len = zis.read(buffer)
                        while (len > 0) {
                            fos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
        }
    }

    private suspend fun parseAndStoreHeartRate(file: File) {
        val heartRates = mutableListOf<HeartRate>()

        file.bufferedReader().use { reader ->
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    try {
                        val timestamp = Instant.parse(parts[0].trim())
                        val heartRate = parts[1].trim().toFloat()
                        val confidence = parts[2].trim().toFloat()

                        heartRates.add(HeartRate(
                            timestamp = timestamp,
                            heartRate = heartRate,
                            confidenceScore = confidence
                        ))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        }

        if (heartRates.isNotEmpty()) {
            database.heartRateDao().insertAll(heartRates)
        }
    }

    private suspend fun parseAndStoreRespiration(file: File) {
        val respirationRates = mutableListOf<RespirationRate>()

        file.bufferedReader().use { reader ->
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    try {
                        val timestamp = Instant.parse(parts[0].trim())
                        val respRate = parts[1].trim().toFloat()
                        val ibi = parts[2].trim().toFloat()

                        respirationRates.add(RespirationRate(
                            timestamp = timestamp,
                            respirationRate = respRate,
                            ibi = ibi
                        ))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        }

        if (respirationRates.isNotEmpty()) {
            database.respirationRateDao().insertAll(respirationRates)
        }
    }

    private suspend fun parseAndStoreEda(file: File) {
        val edaData = mutableListOf<Eda>()

        file.bufferedReader().use { reader ->
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 2) {
                    try {
                        val timestamp = Instant.parse(parts[0].trim())
                        val edaValue = parts[1].trim().toFloat()

                        edaData.add(Eda(
                            timestamp = timestamp,
                            edaValue = edaValue
                        ))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        }

        if (edaData.isNotEmpty()) {
            database.edaDao().insertAll(edaData)
        }
    }

    private suspend fun parseAndStoreTemperature(file: File) {
        val temperatures = mutableListOf<Temperature>()

        file.bufferedReader().use { reader ->
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 2) {
                    try {
                        val timestamp = Instant.parse(parts[0].trim())
                        val tempValue = parts[1].trim().toFloat()

                        temperatures.add(Temperature(
                            timestamp = timestamp,
                            tempValue = tempValue
                        ))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        }

        if (temperatures.isNotEmpty()) {
            database.temperatureDao().insertAll(temperatures)
        }
    }

    fun querySensorData(
        sensorType: SensorType,
        from: Instant,
        to: Instant
    ): Flow<List<Any>> {
        return when (sensorType) {
            SensorType.HEART_RATE -> database.heartRateDao().getHeartRateData(from, to) as Flow<List<Any>>
            SensorType.RESPIRATION_RATE -> database.respirationRateDao().getRespirationRateData(from, to) as Flow<List<Any>>
            SensorType.EDA -> database.edaDao().getEdaData(from, to) as Flow<List<Any>>
            SensorType.TEMPERATURE -> database.temperatureDao().getTemperatureData(from, to) as Flow<List<Any>>
        }
    }

    suspend fun cleanupOldData(cutoffDate: Instant) = withContext(Dispatchers.IO) {
        database.heartRateDao().deleteOldData(cutoffDate)
        database.respirationRateDao().deleteOldData(cutoffDate)
        database.edaDao().deleteOldData(cutoffDate)
        database.temperatureDao().deleteOldData(cutoffDate)
    }
}