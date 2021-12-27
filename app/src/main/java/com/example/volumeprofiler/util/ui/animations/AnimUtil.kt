package com.example.volumeprofiler.util.ui.animations

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.CycleInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation

class AnimUtil {

    companion object {

        fun shakeAnimation(view: View): Unit {
            val translateAnimation: TranslateAnimation = TranslateAnimation(
                    0f, 60f, 0f, 0f
            )
            translateAnimation.duration = 500
            translateAnimation.interpolator = CycleInterpolator(3f)
            view.startAnimation(translateAnimation)
        }

        fun scaleAnimation(view: View, up: Boolean): Unit {
            val fromX: Float = if (up) 0.0f else 1.0f
            val toX: Float = if (up) 1.0f else 0.0f
            val fromY: Float = if (up) 0.0f else 1.0f
            val toY: Float = if (up) 1.0f else 0.0f
            val animation = ScaleAnimation(fromX, toX, fromY, toY,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
            animation.duration = 300
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                    Log.i("AnimationUtils", "onAnimationRepeat")
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (up) {
                        view.visibility = View.VISIBLE
                    }
                    else {
                        view.visibility = View.INVISIBLE
                    }
                }

                override fun onAnimationStart(animation: Animation?) {
                    Log.i("AnimationUtils", "onAnimationStart")
                }
            })
            view.startAnimation(animation)
        }

        fun selectedItemAnimation(itemView: View, selected: Boolean): Unit {
            val value: Float = if (selected) 0.8f else 1.0f
            val x: PropertyValuesHolder = PropertyValuesHolder.ofFloat(View.SCALE_X, value)
            val y: PropertyValuesHolder = PropertyValuesHolder.ofFloat(View.SCALE_Y, value)
            val animator = ObjectAnimator.ofPropertyValuesHolder(itemView, x, y)
            animator.duration = 300
            animator.start()
        }
    }
}