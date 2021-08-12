package com.example.volumeprofiler.views

import android.widget.FrameLayout
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class LayoutWrapper : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var onTouch: ((event :MotionEvent) -> Unit)? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        onTouch?.invoke(event)
        return super.dispatchTouchEvent(event)
    }
}