package com.parkwhere.scanner.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "parking_records")
data class ParkingRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "address") val address: String? = null
) : Parcelable

@Entity(tableName = "blacklist_items")
data class BlacklistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "blacklisted_at") val blacklistedAt: Date = Date()
)