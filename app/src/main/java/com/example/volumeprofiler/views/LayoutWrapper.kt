package com.example.volumeprofiler.views

import android.widget.FrameLayout
import android.content.Context
import android.view.MotionEvent

class LayoutWrapper(context: Context): FrameLayout(context) {

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        return super.onTouchEvent(motionEvent)
    }
}