package ru.rrtry.silentdroid.ui

import android.animation.*
import android.view.View
import android.view.View.*
import android.view.ViewAnimationUtils
import android.view.animation.*
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.Integer.max

object Animations {

    fun getFabAnimation(fab: FloatingActionButton, visible: Boolean): ValueAnimator {
        return if (visible) {
            getFabCollapseAnimation(fab)
        } else {
            getFabExpandAnimation(fab)
        }
    }

    private fun getFabExpandAnimation(fab: FloatingActionButton): ValueAnimator {
        return ValueAnimator.ofFloat(fab.rotation, 90f).apply {
            addUpdateListener {
                fab.rotation = it.animatedValue as Float
            }
        }
    }

    private fun getFabCollapseAnimation(fab: FloatingActionButton): ValueAnimator {
        return ValueAnimator.ofFloat(fab.rotation, 0f).apply {
            addUpdateListener {
                fab.rotation = it.animatedValue as Float
            }
        }
    }

    fun getMenuOptionAnimation(startView: View, target: View): Animator {
        return if (target.visibility == VISIBLE) {
            getOptionHideAnimation(startView, target)
        } else {
            getOptionRevealAnimation(startView, target)
        }
    }

    fun getOverlayCircularAnimator(view: View, origin: View): Animator {
        return if (view.visibility == INVISIBLE) {
            view.visibility = VISIBLE
            getOverlayCircularRevealAnimator(view, origin)
        } else {
            getOverlayCircularHideAnimator(view, origin)
        }
    }

    private fun getOverlayCircularHideAnimator(view: View, origin: View): Animator {
        return ViewAnimationUtils.createCircularReveal(
            view,
            (origin.x + origin.width / 2).toInt(),
            (origin.y + origin.height / 2).toInt(),
            max(view.width, view.height) * 2f,
            0f
        ).apply {
            doOnEnd {
                view.visibility = INVISIBLE
            }
            duration = 500
        }
    }

    private fun getOverlayCircularRevealAnimator(view: View, origin: View): Animator {
        return ViewAnimationUtils.createCircularReveal(
            view,
            (origin.x + origin.width / 2).toInt(),
            (origin.y + origin.height / 2).toInt(),
            0f,
            max(view.width, view.height) * 2f
        ).apply {
            doOnStart {
                view.visibility = VISIBLE
            }
            doOnEnd {
                view.visibility = VISIBLE
            }
            duration = 500
        }
    }

    private fun getOptionHideAnimation(expandableView: View, option: View): ObjectAnimator {

        val alpha = PropertyValuesHolder.ofFloat(ALPHA, 1f, 0f)
        return ObjectAnimator.ofPropertyValuesHolder(option, alpha).apply {
            doOnCancel {
                option.visibility = INVISIBLE
            }
            doOnEnd {
                option.visibility = INVISIBLE
            }
        }
    }

    private fun getOptionRevealAnimation(expandableView: View, option: View): ObjectAnimator {

        val alpha = PropertyValuesHolder.ofFloat(ALPHA, 0f, 1f)

        return ObjectAnimator.ofPropertyValuesHolder(option, alpha).apply {

            doOnStart {
                option.visibility = VISIBLE
            }
            doOnCancel {
                option.visibility = VISIBLE
            }
            doOnEnd {
                option.visibility = VISIBLE
            }
        }
    }

    fun shake(view: View): Unit {
        TranslateAnimation(
            0f, 30f, 0f, 0f
        ).apply {
            duration = 500
            interpolator = CycleInterpolator(3f)
            view.startAnimation(this)
        }
    }

    fun scale(view: View, up: Boolean) {

        val fromX: Float = if (up) 0.0f else 1.0f
        val toX: Float = if (up) 1.0f else 0.0f
        val fromY: Float = if (up) 0.0f else 1.0f
        val toY: Float = if (up) 1.0f else 0.0f

        val animation = ScaleAnimation(fromX, toX, fromY, toY,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f)

        animation.duration = 300

        animation.setAnimationListener(object : SimpleAnimationListener() {

            override fun onAnimationEnd(animation: Animation?) {
                if (up) {
                    view.visibility = VISIBLE
                }
                else {
                    view.visibility = INVISIBLE
                }
            }
        })
        view.startAnimation(animation)
    }

    fun selected(itemView: View, selected: Boolean) {

        val value: Float = if (selected) 0.8f else 1.0f

        val x: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_X, value)
        val y: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_Y, value)

        ObjectAnimator.ofPropertyValuesHolder(itemView, x, y).apply {
            duration = 300
            start()
        }
    }
}