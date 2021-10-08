package com.example.volumeprofiler.util

import android.content.Context
import android.graphics.Rect
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

class ViewUtil {

    companion object {

        fun calculateHalfExpandedRatio(rootViewGroup: ViewGroup, targetView: View): Float {
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100f / rootViewGroup.height) / 100
        }

        fun hideSoftInputFromWindow(context: Context, windowToken: IBinder?): Unit {
            val inputMethodService: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodService.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}