package com.example.volumeprofiler.database

import android.net.Uri
import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.ArrayList

class Converters {

    @TypeConverter
    fun toBoolean(byte: Byte): Boolean {
        return byte == 1.toByte()
    }

    @TypeConverter
    fun fromBoolean(bool: Boolean): Byte {
        return if (bool) {
            1.toByte()
        } else {
            0.toByte()
        }
    }

    @TypeConverter
    fun toArrayList(string: String): ArrayList<Int> {
        return string.split(',').mapNotNull {
            try {
                it.toInt()
            }
            catch (e: NumberFormatException) {
                null
            }
        } as ArrayList<Int>
    }

    @TypeConverter
    fun fromArrayList(arrayList: ArrayList<Int>): String {
        return arrayList.joinToString(",")
    }

    @TypeConverter
    fun toList(string: String): List<Int> {
        return string.split(',').mapNotNull {
            try {
                it.toInt()
            }
            catch (e: NumberFormatException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String {
        return uuid.toString()
    }

    @TypeConverter
    fun fromDate(date: LocalDateTime): Long {
        return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): LocalDateTime? {
        return millisSinceEpoch?.let {
            Instant.ofEpochMilli(millisSinceEpoch).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri): String = uri.toString()

    @TypeConverter
    fun toUri(uriString: String): Uri = Uri.parse(uriString)
}