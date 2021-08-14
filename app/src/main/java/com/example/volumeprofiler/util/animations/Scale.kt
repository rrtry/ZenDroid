package com.example.volumeprofiler.util.animations

import android.animation.*
import android.view.View
import android.view.ViewGroup
import androidx.transition.*

class Scale: Visibility() {

    override fun captureStartValues(transitionValues: TransitionValues) {
        super.captureStartValues(transitionValues)
        transitionValues.values[PROPNAME_TRANSITION_SCALE_X] = transitionValues.view.scaleX
        transitionValues.values[PROPNAME_TRANSITION_SCALE_Y] = transitionValues.view.scaleY
    }

    override fun onAppear(sceneRoot: ViewGroup?,
                          view: View?,
                          startValues: TransitionValues?,
                          endValues: TransitionValues?): Animator {
        super.onAppear(sceneRoot, view, startValues, endValues)
        val startScaleX = getStartScaleValue(startValues, PROPNAME_TRANSITION_SCALE_X, 0f)
        val startScaleY = getStartScaleValue(startValues, PROPNAME_TRANSITION_SCALE_Y, 0f)
        return createScaleAnimation(
                view!!,
                if (startScaleX == 1f) 0f else startScaleX,
                if (startScaleY == 1f) 0f else startScaleY,
                1f,
                1f
        )
    }

    override fun onDisappear(
            sceneRoot: ViewGroup?,
            view: View?,
            startValues: TransitionValues?,
            endValues: TransitionValues?
    ): Animator {
        super.onDisappear(sceneRoot, view, startValues, endValues)
        val startScaleX = getStartScaleValue(startValues, PROPNAME_TRANSITION_SCALE_X, 1f)
        val startScaleY = getStartScaleValue(startValues, PROPNAME_TRANSITION_SCALE_Y, 1f)
        return createScaleAnimation(
                view!!,
                if (startScaleX == 0f) 1f else startScaleX,
                if (startScaleY == 0f) 1f else startScaleY,
                0.1f,
                0.1f
        )
    }

    private fun setStartValues(view: View, startScaleX: Float, startScaleY: Float): Unit {
        view.scaleX = startScaleX
        view.scaleY = startScaleY
    }

    private fun createScaleAnimation(view: View, startScaleX: Float, startScaleY: Float, endScaleX: Float, endScaleY: Float): Animator {
        setStartValues(view, startScaleX, startScaleY)
        val animScaleX: ObjectAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, startScaleX, endScaleX)
        val animScaleY: ObjectAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScaleY, endScaleY)
        val animator: AnimatorSet = AnimatorSet()
        animator.duration = 300
        animator.playTogether(animScaleX, animScaleY)
        view.let {
            animator.addListener(getAnimatorListener(it))
            addListener(getTransitionListener(it))
        }
        return animator
    }

    private fun getStartScaleValue(scaleValue: TransitionValues?, propName: String, fallbackValue: Float): Float {
        return scaleValue?.values?.get(propName) as? Float ?: fallbackValue
    }

    private fun getAnimatorListener(view: View): AnimatorListenerAdapter {
        return object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                doOnEnd(view)
            }
        }
    }

    private fun getTransitionListener(view: View): TransitionListenerAdapter {
        return object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                doOnEnd(view)
            }
        }
    }

    private fun doOnEnd(view: View) {
        view.scaleX = 1f
        view.scaleY = 1f
    }

    companion object {
        private const val PROPNAME_TRANSITION_SCALE_X = "scaleX"
        private const val PROPNAME_TRANSITION_SCALE_Y = "scaleY"
    }
}