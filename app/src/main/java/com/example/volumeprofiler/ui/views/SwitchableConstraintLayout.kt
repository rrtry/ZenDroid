package com.example.volumeprofiler.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class SwitchableConstraintLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    var disabled = false
        set(value) {
            field = value
            requestLayout()
        }

    private val paint = Paint()
    init {
        val colorMatrix = ColorMatrix()
        colorMatrix.set(
                floatArrayOf(
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0.0f, 0.0f, 0.0f, 0.75f, 0f
                )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return disabled
    }

    private fun drawToOffScreenBuffer(canvas: Canvas?): Unit {
        if (disabled) {
            canvas?.saveLayer(null, paint)
        }
    }

    private fun restoreFromOffScreenBuffer(canvas: Canvas?): Unit {
        if (disabled) {
            canvas?.restore()
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        drawToOffScreenBuffer(canvas)
        super.dispatchDraw(canvas)
        restoreFromOffScreenBuffer(canvas)
    }

    override fun draw(canvas: Canvas?) {
        drawToOffScreenBuffer(canvas)
        super.draw(canvas)
        restoreFromOffScreenBuffer(canvas)
    }
}