package com.example.volumeprofiler.util.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import android.view.ViewGroup
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FloatingActionMenuController {

    var isVisible: Boolean = false
    private set

    private var isAnimationRunning: Boolean = false

    private fun applyTransformation(
        expandableFab: FloatingActionButton,
        menuOptions: List<View>,
        overlay: ViewGroup
    ) {
        if (!isAnimationRunning) {

            overlay.isClickable = !isVisible
            overlay.isFocusable = !isVisible

            AnimatorSet().apply {
                play(AnimUtil.getFabAnimation(expandableFab, isVisible))
                playTogether(menuOptions.map {
                    when (it) {
                        is ViewGroup -> {
                            AnimUtil.getOverlayCircularAnimator(it, expandableFab)
                        }
                        else -> {
                            AnimUtil.getMenuOptionAnimation(expandableFab, it)
                        }
                    }
                })
                addListener(object : Animator.AnimatorListener {

                    override fun onAnimationStart(animation: Animator?) {
                        isAnimationRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        isVisible = !isVisible
                        isAnimationRunning = false
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        isVisible = !isVisible
                        isAnimationRunning = false
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                        isAnimationRunning = true
                    }
                })
                start()
            }
        }
    }

    fun toggleVisibility(
        expandableFab: FloatingActionButton,
        menuOptions: List<View>,
        overlay: ViewGroup
    ) {
        applyTransformation(expandableFab, menuOptions, overlay)
    }
}