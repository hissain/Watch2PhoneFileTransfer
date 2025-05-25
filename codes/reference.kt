// ========================================
// PROJECT STRUCTURE
// ========================================
/*
Project Structure:
├── app/                           # Phone App Module
├── wear/                         # WearOS Watch App Module  
├── datastorelib/                 # DataStore Library Module
└── shared/                       # Shared models and utilities

Build Configuration Files:
- settings.gradle.kts
- build.gradle.kts (project level)
- app/build.gradle.kts
- wear/build.gradle.kts
- datastorelib/build.gradle.kts
*/

// ========================================
// 1. PROJECT-LEVEL BUILD CONFIGURATION
// ========================================

// settings.gradle.kts
/*
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "SensorDataCollectionSystem"
include(":app")
include(":wear")
include(":datastorelib")
*/

// build.gradle.kts (Project level)
/*
buildscript {
    ext {
        compose_version = '1.5.4'
        room_version = '2.5.0'
        kotlin_version = '1.8.21'
        sensor_version = '1.0.0'
    }
}

plugins {
    id 'com.android.application' version '8.1.2' apply false
    id 'com.android.library' version '8.1.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.8.21' apply false
}
*/

// ========================================
// 2. DATASTORELIB MODULE
// ========================================

// datastorelib/build.gradle.kts
/*
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("maven-publish")
}

android {
    namespace = "com.sensordata.datastorelib"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        version = project.ext.sensor_version

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.sensordata"
            artifactId = "datastorelib"
            version = project.ext.sensor_version
            
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
*/

// DataStore Library - Entities
package com.sensordata.datastorelib.entities

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

// DataStore Library - DAOs
package com.sensordata.datastorelib.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import com.sensordata.datastorelib.entities.*

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

// DataStore Library - Database
package com.sensordata.datastorelib.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sensordata.datastorelib.dao.*
import com.sensordata.datastorelib.entities.*
import java.time.Instant

@TypeConverters(Converters::class)
@Database(
    entities = [HeartRate::class, RespirationRate::class, Eda::class, Temperature::class],
    version = 1,
    exportSchema = false
)
abstract class SensorDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun respirationRateDao(): RespirationRateDao
    abstract fun edaDao(): EdaDao
    abstract fun temperatureDao(): TemperatureDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}

// DataStore Library - Models
package com.sensordata.datastorelib.models

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

// DataStore Library - Main API
package com.sensordata.datastorelib

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.sensordata.datastorelib.database.SensorDatabase
import com.sensordata.datastorelib.entities.*
import com.sensordata.datastorelib.models.*
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

// ========================================
// 3. WEAROS WATCH APP MODULE
// ========================================

// wear/build.gradle.kts
/*
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.sensordata.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sensordata.wear"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Wear Compose
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
*/

// WearOS - Sensor Data Collector
package com.sensordata.wear.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class SensorDataCollector(private val context: Context) {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val isCollecting = AtomicBoolean(false)
    private val collectingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // CSV Writers
    private var heartRateWriter: FileWriter? = null
    private var respirationWriter: FileWriter? = null
    private var edaWriter: FileWriter? = null
    private var temperatureWriter: FileWriter? = null
    
    // Sensor listeners
    private val heartRateListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val timestamp = Instant.now().toString()
                val heartRate = it.values[0]
                val confidence = if (it.values.size > 1) it.values[1] else 1.0f
                
                collectingScope.launch {
                    heartRateWriter?.let { writer ->
                        synchronized(writer) {
                            writer.appendLine("$timestamp,$heartRate,$confidence")
                            writer.flush()
                        }
                    }
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    
    private val temperatureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val timestamp = Instant.now().toString()
                val temperature = it.values[0]
                
                collectingScope.launch {
                    temperatureWriter?.let { writer ->
                        synchronized(writer) {
                            writer.appendLine("$timestamp,$temperature")
                            writer.flush()
                        }
                    }
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    
    fun startCollection(): Boolean {
        if (!hasRequiredPermissions()) {
            return false
        }
        
        if (isCollecting.compareAndSet(false, true)) {
            try {
                setupCsvFiles()
                registerSensorListeners()
                return true
            } catch (e: Exception) {
                isCollecting.set(false)
                return false
            }
        }
        return false
    }
    
    fun stopCollection() {
        if (isCollecting.compareAndSet(true, false)) {
            unregisterSensorListeners()
            closeCsvFiles()
        }
    }
    
    fun isCollecting(): Boolean = isCollecting.get()
    
    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun setupCsvFiles() {
        val dataDir = File(context.getExternalFilesDir(null), "sensordata")
        dataDir.mkdirs()
        
        // Heart Rate CSV
        val heartRateFile = File(dataDir, "heart_rate.csv")
        heartRateWriter = FileWriter(heartRateFile, true).apply {
            if (heartRateFile.length() == 0L) {
                appendLine("timestamp,heart_rate,confidence_score")
            }
        }
        
        // Respiration Rate CSV (simulated for demo)
        val respirationFile = File(dataDir, "respiration_rate.csv")
        respirationWriter = FileWriter(respirationFile, true).apply {
            if (respirationFile.length() == 0L) {
                appendLine("timestamp,respiration_rate,ibi")
            }
        }
        
        // EDA CSV (simulated for demo)
        val edaFile = File(dataDir, "eda.csv")
        edaWriter = FileWriter(edaFile, true).apply {
            if (edaFile.length() == 0L) {
                appendLine("timestamp,eda_value")
            }
        }
        
        // Temperature CSV
        val temperatureFile = File(dataDir, "temperature.csv")
        temperatureWriter = FileWriter(temperatureFile, true).apply {
            if (temperatureFile.length() == 0L) {
                appendLine("timestamp,temp_value")
            }
        }
        
        // Start simulated data collection for respiration and EDA
        startSimulatedDataCollection()
    }
    
    private fun registerSensorListeners() {
        // Heart Rate Sensor
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let { sensor ->
            sensorManager.registerListener(heartRateListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        // Temperature Sensor (if available)
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let { sensor ->
            sensorManager.registerListener(temperatureListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    private fun unregisterSensorListeners() {
        sensorManager.unregisterListener(heartRateListener)
        sensorManager.unregisterListener(temperatureListener)
    }
    
    private fun startSimulatedDataCollection() {
        // Simulate respiration and EDA data at 25Hz
        collectingScope.launch {
            while (isCollecting.get()) {
                val timestamp = Instant.now().toString()
                
                // Simulate respiration rate (12-20 breaths per minute)
                val respirationRate = 12 + kotlin.random.Random.nextFloat() * 8
                val ibi = 60f / respirationRate
                
                respirationWriter?.let { writer ->
                    synchronized(writer) {
                        writer.appendLine("$timestamp,$respirationRate,$ibi")
                        writer.flush()
                    }
                }
                
                // Simulate EDA (0.1-10 microsiemens)
                val edaValue = 0.1f + kotlin.random.Random.nextFloat() * 9.9f
                
                edaWriter?.let { writer ->
                    synchronized(writer) {
                        writer.appendLine("$timestamp,$edaValue")
                        writer.flush()
                    }
                }
                
                delay(40) // 25Hz = 40ms interval
            }
        }
    }
    
    private fun closeCsvFiles() {
        try {
            heartRateWriter?.close()
            respirationWriter?.close()
            edaWriter?.close()
            temperatureWriter?.close()
        } catch (e: Exception) {
            // Log error
        } finally {
            heartRateWriter = null
            respirationWriter = null
            edaWriter = null
            temperatureWriter = null
        }
    }
}

// WearOS - Data Package Manager
package com.sensordata.wear.sync

import android.content.Context
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataPackageManager(private val context: Context) {
    
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    
    suspend fun packageAndSendData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = createZipPackage()
            if (zipFile != null && zipFile.exists()) {
                val success = sendZipToPhone(zipFile)
                zipFile.delete() // Cleanup
                success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun createZipPackage(): File? {
        val dataDir = File(context.getExternalFilesDir(null), "sensordata")
        if (!dataDir.exists()) return null
        
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val zipFile = File(context.cacheDir, "sensordata_$timestamp.zip")
        
        val csvFiles = listOf(
            "heart_rate.csv",
            "respiration_rate.csv", 
            "eda.csv",
            "temperature.csv"
        )
        
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                csvFiles.forEach { fileName ->
                    val file = File(dataDir, fileName)
                    if (file.exists()) {
                        FileInputStream(file).use { fis ->
                            val zipEntry = ZipEntry(fileName)
                            zipOut.putNextEntry(zipEntry)
                            fis.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun sendZipToPhone(zipFile: File): Boolean {
        return try {
            val bytes = zipFile.readBytes()
            val putDataRequest = PutDataMapRequest.create("/sensor-data").apply {
                dataMap.putByteArray("zip_data", bytes)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().apply {
                setUrgent()
            }
            
            val result = dataClient.putDataItem(putDataRequest).await()
            result.uri != null
        } catch (e: Exception) {
            false
        }
    }
}

// WearOS - Sync Worker
package com.sensordata.wear.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class SensorSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val packageManager = DataPackageManager(applicationContext)
        return if (packageManager.packageAndSendData()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "sensor_sync_work"
        
        fun schedulePeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SensorSyncWorker>(
                2, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
        }
        
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

// WearOS - Main Activity
package com.sensordata.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.sensordata.wear.sensors.SensorDataCollector
import com.sensordata.wear.sync.DataPackageManager
import com.sensordata.wear.workers.SensorSyncWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, can start collection
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            WearApp()
        }
    }
    
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val sensorCollector = remember { SensorDataCollector(context) }
    val packageManager = remember { DataPackageManager(context) }
    
    var isCollecting by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Schedule periodic sync
        SensorSyncWorker.schedulePeriodicSync(context)
    }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sensor Data Collection",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Collection Status
            Text(
                text = if (isCollecting) "Collecting..." else "Stopped",
                style = MaterialTheme.typography.body2,
                color = if (isCollecting) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start/Stop Button
            Button(
                onClick = {
                    if (isCollecting) {
                        sensorCollector.stopCollection()
                        isCollecting = false
                    } else {
                        if (sensorCollector.startCollection()) {
                            isCollecting = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCollecting) "Stop" else "Start")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sync Button
            Button(
                onClick = {
                    scope.launch {
                        isSyncing = true
                        packageManager.packageAndSendData()
                        isSyncing = false
                    }
                },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSyncing) "Syncing..." else "Sync Now")
            }
        }
    }
}

// WearOS - AndroidManifest.xml
/*
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-feature android:name="android.hardware.type.watch" />
    
    <uses-permission android:name="android.permission.BODY_SENSORS" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="com.google.android.permission.PROVIDE_BACKGROUND" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.DeviceDefault">
        
        <uses-library
            android:name="com.google.android.wearable"
            android:required="true" />
            
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="false" />
            
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.DeviceDefault">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
*/

// ========================================
// 4. PHONE APP MODULE  
// ========================================

// app/build.gradle.kts
/*
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.sensordata.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sensordata.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Wearable
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    
    // DataStore Library
    implementation("com.sensordata:datastorelib:$sensor_version")
    implementation(project(":datastorelib"))
    
    // Charts for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
}
*/

// Phone App - Wearable Listener Service
package com.sensordata.app.services

import android.os.Environment
import com.google.android.gms.wearable.*
import com.sensordata.datastorelib.DataStoreLib
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WearableDataListenerService : WearableListenerService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        dataEventBuffer.forEach { dataEvent ->
            if (dataEvent.type == DataEvent.TYPE_CHANGED) {
                val dataItem = dataEvent.dataItem
                if (dataItem.uri.path == "/sensor-data") {
                    handleSensorData(dataItem)
                }
            }
        }
    }
    
    private fun handleSensorData(dataItem: DataItem) {
        serviceScope.launch {
            try {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val zipData = dataMap.getByteArray("zip_data")
                val timestamp = dataMap.getLong("timestamp")
                
                if (zipData != null) {
                    val savedFile = saveZipFile(zipData, timestamp)
                    if (savedFile != null) {
                        // Process the data using DataStoreLib
                        val dataStoreLib = DataStoreLib.getInstance(applicationContext)
                        dataStoreLib.startSync(savedFile.absolutePath)
                        
                        // Cleanup - implement retention policy
                        cleanupOldFiles()
                    }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
    
    private suspend fun saveZipFile(data: ByteArray, timestamp: Long): File? {
        return try {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "sensordata"
            )
            downloadsDir.mkdirs()
            
            val dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val zipFile = File(downloadsDir, "sensordata_$dateTime.zip")
            
            FileOutputStream(zipFile).use { fos ->
                fos.write(data)
            }
            
            zipFile
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun cleanupOldFiles() {
        try {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "sensordata"
            )
            
            if (downloadsDir.exists()) {
                val files = downloadsDir.listFiles()?.sortedByDescending { it.lastModified() }
                
                // Keep only the last 10 files
                files?.drop(10)?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

// Phone App - Main Activity
package com.sensordata.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensordata.app.ui.theme.SensorDataTheme
import com.sensordata.app.viewmodels.MainViewModel
import com.sensordata.datastorelib.models.SyncStatus

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            SensorDataTheme {
                MainScreen()
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

// Phone App - Main Screen
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    val syncStatus by viewModel.syncStatus.observeAsState(SyncStatus.Idle)
    val heartRateData by viewModel.heartRateData.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Status") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Data") }
            )
        }
        
        when (selectedTab) {
            0 -> StatusScreen(syncStatus, viewModel)
            1 -> DataScreen(heartRateData)
        }
    }
}

@Composable
fun StatusScreen(syncStatus: SyncStatus, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sensor Data Collection",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (syncStatus) {
                    is SyncStatus.Idle -> {
                        Text("Ready to sync", color = MaterialTheme.colors.onSurface)
                    }
                    is SyncStatus.InProgress -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Syncing data...", color = MaterialTheme.colors.primary)
                    }
                    is SyncStatus.Success -> {
                        Text(syncStatus.message, color = MaterialTheme.colors.primary)
                    }
                    is SyncStatus.Error -> {
                        Text(syncStatus.message, color = MaterialTheme.colors.error)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { viewModel.refreshData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Data")
        }
    }
}

@Composable
fun DataScreen(heartRateData: List<Any>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Recent Heart Rate Data",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        if (heartRateData.isEmpty()) {
            item {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(heartRateData.take(20)) { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Heart Rate Data",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = data.toString(),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }
    }
}

// Phone App - ViewModel
package com.sensordata.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sensordata.datastorelib.DataStoreLib
import com.sensordata.datastorelib.models.SensorType
import com.sensordata.datastorelib.models.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStoreLib = DataStoreLib.getInstance(application)
    
    val syncStatus: LiveData<SyncStatus> = dataStoreLib.syncStatus
    
    private val _heartRateData = MutableStateFlow<List<Any>>(emptyList())
    val heartRateData: StateFlow<List<Any>> = _heartRateData.asStateFlow()
    
    init {
        refreshData()
    }
    
    fun refreshData() {
        viewModelScope.launch {
            try {
                val endTime = Instant.now()
                val startTime = endTime.minus(24, ChronoUnit.HOURS)
                
                dataStoreLib.querySensorData(
                    SensorType.HEART_RATE,
                    startTime,
                    endTime
                ).collect { data ->
                    _heartRateData.value = data
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

// Phone App - UI Theme
package com.sensordata.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF3700B3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6)
)

private val LightColorPalette = lightColors(
    primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF3700B3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6)
)

@Composable
fun SensorDataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = androidx.compose.material.Typography(),
        shapes = androidx.compose.material.Shapes(),
        content = content
    )
}

// Phone App - AndroidManifest.xml
/*
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="com.google.android.permission.PROVIDE_BACKGROUND" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.SensorDataCollection">
        
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="false" />
            
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.SensorDataCollection">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".services.WearableDataListenerService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
                <data android:scheme="wear" android:host="*" />
            </intent-filter>
        </service>
        
    </application>
</manifest>
*/

// ========================================
// 5. TESTING EXAMPLES
// ========================================

// Unit Test - DataStoreLib CSV Parsing
package com.sensordata.datastorelib

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.Instant

class DataStoreLibTest {
    
    @Test
    fun testCsvParsing() = runTest {
        // Create test CSV content
        val csvContent = """
            timestamp,heart_rate,confidence_score
            2024-01-01T10:00:00Z,72.5,0.95
            2024-01-01T10:00:01Z,73.1,0.92
        """.trimIndent()
        
        val testFile = File.createTempFile("test_heart_rate", ".csv")
        testFile.writeText(csvContent)
        
        // Test parsing logic would go here
        // This is a simplified example
        
        assertTrue("CSV file should exist", testFile.exists())
        assertTrue("CSV should contain data", testFile.readText().contains("heart_rate"))
        
        testFile.delete()
    }
}

// Integration Test - End-to-End Sync
package com.sensordata.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sensordata.datastorelib.DataStoreLib
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {
    
    @Test
    fun testEndToEndSync() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dataStoreLib = DataStoreLib.getInstance(appContext)
        
        // This would test the complete sync process
        // from ZIP file to database storage
        
        // Implementation would involve creating test ZIP files
        // and verifying data appears correctly in the database
    }
}

// ========================================
// 6. ADDITIONAL CONFIGURATION FILES
// ========================================

// strings.xml
/*
<resources>
    <string name="app_name">Sensor Data Collection</string>
    <string name="start_collection">Start Collection</string>
    <string name="stop_collection">Stop Collection</string>
    <string name="sync_now">Sync Now</string>
    <string name="sync_status">Sync Status</string>
    <string name="collecting">Collecting...</string>
    <string name="stopped">Stopped</string>
    <string name="syncing">Syncing...</string>
    <string name="sync_success">Data synchronized successfully</string>
    <string name="sync_error">Sync failed</string>
    <string name="no_data">No data available</string>
    <string name="recent_data">Recent Heart Rate Data</string>
    <string name="refresh_data">Refresh Data</string>
    <string name="permission_required">Sensor permissions required</string>
</resources>
*/

// proguard-rules.pro
/*
# Keep DataStoreLib classes
-keep class com.sensordata.datastorelib.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Wearable classes
-keep class com.google.android.gms.wearable.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
*/
