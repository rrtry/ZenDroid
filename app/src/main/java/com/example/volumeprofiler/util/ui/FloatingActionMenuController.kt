package com.example.volumeprofiler.util.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import android.view.ViewGroup
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference

class FloatingActionMenuController(listenerRef: WeakReference<MenuStateListener>, isVisible: Boolean) {

    interface MenuStateListener {

        fun onTransformationFinished()
    }

    val listener: MenuStateListener = listenerRef.get()!!
    var isVisible: Boolean = isVisible
    private set

    private var isAnimationRunning: Boolean = false

    fun toggle(
        expandableFab: FloatingActionButton,
        vararg menuOptions: View
    ) {
        if (!isAnimationRunning) {

            val overlay: ViewGroup = menuOptions.firstOrNull {
                it is ViewGroup
            } as ViewGroup

            overlay.isClickable = !isVisible
            overlay.isFocusable = !isVisible

            AnimatorSet().apply {
                play(AnimUtil.getFabAnimation(expandableFab, isVisible))
                playTogether(menuOptions.map {
                    when (it) {
                        is ViewGroup -> AnimUtil.getOverlayCircularAnimator(it, expandableFab)
                        else -> AnimUtil.getMenuOptionAnimation(expandableFab, it)
                    }
                })
                addListener(object : Animator.AnimatorListener {

                    override fun onAnimationStart(animation: Animator?) {
                        isAnimationRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        isVisible = !isVisible
                        isAnimationRunning = false
                        listener.onTransformationFinished()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        isVisible = !isVisible
                        isAnimationRunning = false
                        listener.onTransformationFinished()
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                        isAnimationRunning = true
                    }
                })
                start()
            }
        }
    }
}