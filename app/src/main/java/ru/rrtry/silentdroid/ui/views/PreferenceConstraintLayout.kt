package ru.rrtry.silentdroid.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class PreferenceConstraintLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val paint = Paint()
    var disabled = false
        set(value) {
            field = value
            requestLayout()
        }

    init {
        val colorMatrix: ColorMatrix = ColorMatrix()
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

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = disabled

    private fun saveLayer(canvas: Canvas?) {
        if (disabled) canvas?.saveLayer(null, paint)
    }

    private fun restoreLayer(canvas: Canvas?) {
        if (disabled) canvas?.restore()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        saveLayer(canvas)
        super.dispatchDraw(canvas)
        restoreLayer(canvas)
    }

    override fun draw(canvas: Canvas?) {
        saveLayer(canvas)
        super.draw(canvas)
        restoreLayer(canvas)
    }
}