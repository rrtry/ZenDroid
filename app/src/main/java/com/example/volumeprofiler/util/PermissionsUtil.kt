package com.example.volumeprofiler.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun checkSelfPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}