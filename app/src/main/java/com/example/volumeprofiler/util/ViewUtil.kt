package com.example.volumeprofiler.util

import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ViewUtil {

    companion object {

        fun calculateHalfExpandedRatio(context: Context, rootViewGroup: ViewGroup, targetView: View): Float {
            val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay: Display = windowManager.defaultDisplay
            val displayMetrics: DisplayMetrics = DisplayMetrics()
            defaultDisplay.getMetrics(displayMetrics)
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100f / rootViewGroup.height) / 100
        }
    }
}