package com.example.volumeprofiler.util.ui.animations

import android.animation.*
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.*
import android.view.animation.Animation.REVERSE
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton

object AnimUtil {

    fun getFabExpandAnimation(fab: FloatingActionButton): ValueAnimator {
        return ValueAnimator.ofFloat(fab.rotation, 90f).apply {
            addUpdateListener {
                fab.rotation = it.animatedValue as Float
            }
        }
    }

    fun getFabCollapseAnimation(fab: FloatingActionButton): ValueAnimator {
        return ValueAnimator.ofFloat(fab.rotation, 0f).apply {
            addUpdateListener {
                fab.rotation = it.animatedValue as Float
            }
        }
    }

    fun getMenuOptionAnimation(startView: View, target: View): AnimationSet {
        return if (target.isVisible) {
            getOptionHideAnimationSet(startView, target)
        } else {
            getOptionRevealAnimationSet(startView)
        }
    }

    fun getOverlayAnimation(view: View): AlphaAnimation {
        return if (view.isVisible) {
            getOverlayHideAnimation(view)
        } else {
            getOverlayRevealAnimation(view)
        }
    }

    private fun getOverlayHideAnimation(view: View): AlphaAnimation {
        return AlphaAnimation(1f, 0f).apply {

            repeatMode = REVERSE
            duration = 400
            fillAfter = true

            setAnimationListener(object : SimpleAnimationListener() {

                override fun onAnimationEnd(animation: Animation?) {
                    view.visibility = INVISIBLE
                }
            })
        }
    }

    private fun getOverlayRevealAnimation(view: View): AlphaAnimation {
        return AlphaAnimation(0f, 1f).apply {

            repeatMode = REVERSE
            duration = 400
            fillAfter = true

            setAnimationListener(object : SimpleAnimationListener() {

                override fun onAnimationEnd(animation: Animation?) {
                    view.visibility = VISIBLE
                }
            })
        }
    }

    private fun getOptionHideAnimationSet(expandableView: View, option: View): AnimationSet {
        return AnimationSet(false).apply {
            addAnimation(
                TranslateAnimation(
                    0f, 0f, 0f, expandableView.y - option.y
                )
            )
            addAnimation(
                AlphaAnimation(
                    1f, 0f
                )
            )
            repeatMode = REVERSE
            duration = 400
            fillAfter = true
        }
    }

    private fun getOptionRevealAnimationSet(expandableView: View): AnimationSet {
        return AnimationSet(false).apply {
            addAnimation(
                TranslateAnimation(
                    0f, 0f, expandableView.height.toFloat(), 0f
                ).apply {
                    interpolator = OvershootInterpolator(5f)
                }
            )
            addAnimation(
                AlphaAnimation(
                    0f, 1f
                )
            )
            repeatMode = REVERSE
            duration = 400
            fillAfter = true
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

    fun scale(view: View, up: Boolean): Unit {

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

    fun selected(itemView: View, selected: Boolean): Unit {

        val value: Float = if (selected) 0.8f else 1.0f

        val x: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_X, value)
        val y: PropertyValuesHolder = PropertyValuesHolder.ofFloat(SCALE_Y, value)

        ObjectAnimator.ofPropertyValuesHolder(itemView, x, y).apply {
            duration = 300
            start()
        }
    }
}