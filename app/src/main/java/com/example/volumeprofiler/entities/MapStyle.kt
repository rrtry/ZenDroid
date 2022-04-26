package com.example.volumeprofiler.entities

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.IntegerRes

data class MapStyle(
    val title: String,
    val resId: Int,
    val preview: Drawable
)
