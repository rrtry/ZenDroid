package com.example.volumeprofiler.interfaces

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface FabContainer {

    fun onFabClick(fab: FloatingActionButton)

    fun onUpdateFab(fab: FloatingActionButton)

    fun onAnimateFab(fab: FloatingActionButton) {
        val scaleAnimation: ScaleAnimation = ScaleAnimation(
            1f, 0f, 1f, 0f,
            ScaleAnimation.RELATIVE_TO_SELF,
            0.5f,
            ScaleAnimation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            repeatMode = ScaleAnimation.REVERSE
            repeatCount = ScaleAnimation.RESTART
            duration = 250
            setAnimationListener(object : Animation.AnimationListener {

                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {

                }

                override fun onAnimationRepeat(animation: Animation?) {
                    onUpdateFab(fab)
                }
            })
        }
        fab.startAnimation(scaleAnimation)
    }
}