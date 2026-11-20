package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ListingEntity::class, CheckLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ModemRadarDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun checkLogDao(): CheckLogDao

    companion object {
        @Volatile
        private var INSTANCE: ModemRadarDatabase? = null

        fun getInstance(context: Context): ModemRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ModemRadarDatabase::class.java,
                    "modem_radar.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
