package com.example.volumeprofiler.util

import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class ViewUtil {

    companion object {

        private fun calculateHalfExpandedRatio(context: Context, rootViewGroup: ViewGroup, targetView: View): Float {
            val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay: Display = windowManager.defaultDisplay
            val displayMetrics: DisplayMetrics = DisplayMetrics()
            defaultDisplay.getMetrics(displayMetrics)
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100 / rootViewGroup.height).toFloat() / 100
        }
    }
}