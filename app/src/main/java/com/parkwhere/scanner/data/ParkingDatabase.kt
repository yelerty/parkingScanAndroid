package com.parkwhere.scanner.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Dao
interface ParkingDao {
    @Query("SELECT * FROM parking_records ORDER BY created_at DESC")
    suspend fun getAllRecords(): List<ParkingRecord>

    @Query("SELECT * FROM parking_records ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int): List<ParkingRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ParkingRecord)

    @Delete
    suspend fun deleteRecord(record: ParkingRecord)

    @Query("DELETE FROM parking_records")
    suspend fun clearAllRecords()
}

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist_items")
    suspend fun getAllBlacklistItems(): List<BlacklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistItem(item: BlacklistItem)

    @Delete
    suspend fun deleteBlacklistItem(item: BlacklistItem)

    @Query("DELETE FROM blacklist_items WHERE blacklisted_at < :cutoffDate")
    suspend fun removeOldBlacklistItems(cutoffDate: Date)
}

@Database(
    entities = [ParkingRecord::class, BlacklistItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ParkingDatabase : RoomDatabase() {
    abstract fun parkingDao(): ParkingDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        @Volatile
        private var INSTANCE: ParkingDatabase? = null

        fun getDatabase(context: android.content.Context): ParkingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ParkingDatabase::class.java,
                    "parking_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}