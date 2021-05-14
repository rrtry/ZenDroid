package com.example.volumeprofiler

import android.view.View
import android.view.animation.ScaleAnimation

interface AnimImplementation {

    fun scaleUpAnimation(view: View) {
        val animation = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        animation.duration = 400
        view.startAnimation(animation)
    }

    fun scaleDownAnimation(view: View) {
        val animation: ScaleAnimation = ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
        ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        animation.duration = 400
        view.startAnimation(animation)
    }
}