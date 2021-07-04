package com.example.volumeprofiler.util

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.view.View
import android.view.animation.ScaleAnimation
import androidx.annotation.ColorInt

class AnimationUtils {

    companion object {

        fun scaleAnimation(view: View, up: Boolean) {
            val fromX: Float = if (up) 0.0f else 1.0f
            val toX: Float = if (up) 1.0f else 0.0f
            val fromY: Float = if (up) 0.0f else 1.0f
            val toY: Float = if (up) 1.0f else 0.0f
            val animation = ScaleAnimation(fromX, toX, fromY, toY,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
            animation.duration = 400
            view.startAnimation(animation)
        }

        fun selectedItemAnimation(itemView: View, selected: Boolean): Unit {
            val value: Float = if (selected) 0.9f else 1.0f
            @ColorInt val colorInt: Int = if (selected) Color.parseColor("#e8e8e8") else Color.WHITE
            itemView.setBackgroundColor(colorInt)
            val x: PropertyValuesHolder = PropertyValuesHolder.ofFloat(View.SCALE_X, value)
            val y: PropertyValuesHolder = PropertyValuesHolder.ofFloat(View.SCALE_Y, value)
            val animator = ObjectAnimator.ofPropertyValuesHolder(itemView, x, y)
            animator.duration = 300
            animator.start()
        }
    }
}