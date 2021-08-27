package com.example.volumeprofiler.util

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

class ViewUtil {

    companion object {

        fun calculateHalfExpandedRatio(rootViewGroup: ViewGroup, targetView: View): Float {
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100f / rootViewGroup.height) / 100
        }
    }
}