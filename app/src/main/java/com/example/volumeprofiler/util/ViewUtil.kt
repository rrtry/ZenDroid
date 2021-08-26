package com.example.volumeprofiler.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class ViewUtil {

    companion object {

        fun calculateHalfExpandedRatio(rootViewGroup: ViewGroup, targetView: View): Float {
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100f / rootViewGroup.height) / 100
        }
    }
}