package ru.rrtry.silentdroid.ui.activities

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Window
import androidx.viewbinding.ViewBinding

abstract class DetailsSlideTransitionActivity <T: ViewBinding>: ViewBindingActivity<T>() {

    abstract val slideDirection: Int

    override fun setWindowTransitions() {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {

            sharedElementEnterTransition = ChangeBounds()
            sharedElementExitTransition = ChangeBounds()

            TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = TRANSITION_DURATION

                addTransition(Fade())
                addTransition(Slide(slideDirection))

                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)

                enterTransition = this
                exitTransition = this
            }
            allowEnterTransitionOverlap = true
        }
    }

    companion object {

        private const val TRANSITION_DURATION: Long = 350
    }
}