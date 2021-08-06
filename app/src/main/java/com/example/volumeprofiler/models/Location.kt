package com.example.volumeprofiler.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["onExitProfile"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
                      ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["onEnterProfile"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Location(
        @PrimaryKey
        val id: UUID = UUID.randomUUID(),
        var latitude: Double,
        var longitude: Double,
        var address: String,
        @ColumnInfo(index = true) var onExitProfileId: UUID?,
        @ColumnInfo(index = true) var onEnterProfileId: UUID,
        var radius: Int = 100,
): Parcelable