package ru.rrtry.silentdroid.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["onExitProfileId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
                      ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["onEnterProfileId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Location(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo (name = "location_id")
        var id: Int = 0,

        var title: String = "",
        var previewImageId: UUID = UUID.randomUUID(),
        var latitude: Double,
        var longitude: Double,
        var address: String,
        var radius: Float = 100f,
        var enabled: Boolean = false,

        @ColumnInfo(index = true)
        var onExitProfileId: UUID,

        @ColumnInfo(index = true)
        var onEnterProfileId: UUID): Parcelable