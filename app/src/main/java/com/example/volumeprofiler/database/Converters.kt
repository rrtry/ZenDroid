package com.example.volumeprofiler.database

import android.net.Uri
import androidx.room.TypeConverter
import java.time.*
import java.util.*

class Converters {

    @TypeConverter
    fun toInt(bool: Boolean): Int = if (bool) 1 else 0

    @TypeConverter
    fun fromInt(int: Int): Boolean = int == 1

    @TypeConverter
    fun fromLocalDateTime(localDateTime: LocalDateTime?): String? {
        localDateTime?.also {
            return it.toString()
        }
        return null
    }

    @TypeConverter
    fun toLocalDateTime(dateString: String?): LocalDateTime? {
        if (dateString != null && dateString != "null") {
            return LocalDateTime.parse(dateString)
        }
        return null
    }

    @TypeConverter
    fun toBoolean(byte: Byte): Boolean = byte == 1.toByte()

    @TypeConverter
    fun fromBoolean(bool: Boolean): Byte = if (bool) 1.toByte() else 0.toByte()

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = UUID.fromString(uuid)

    @TypeConverter
    fun fromUUID(uuid: UUID?): String = uuid.toString()

    @TypeConverter
    fun fromZoneId(zoneId: ZoneId): String = zoneId.toString()

    @TypeConverter
    fun toZoneId(string: String): ZoneId = ZoneId.of(string)

    @TypeConverter
    fun fromLocalTime(localTime: LocalTime): String = localTime.toString()

    @TypeConverter
    fun toLocalTime(string: String): LocalTime = LocalTime.parse(string)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun fromUri(uri: Uri): String = uri.toString()

    @TypeConverter
    fun toUri(uriString: String): Uri = Uri.parse(uriString)
}