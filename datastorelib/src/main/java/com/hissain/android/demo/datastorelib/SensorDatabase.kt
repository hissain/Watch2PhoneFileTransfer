package com.hissain.android.demo.datastorelib

import androidx.room.*
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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