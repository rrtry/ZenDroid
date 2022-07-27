package ru.rrtry.silentdroid.entities

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class MapType(
    val title: String,
    val type: Int,
    val preview: Bitmap
)
