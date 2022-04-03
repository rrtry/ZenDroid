package com.example.volumeprofiler.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout

class ExpandableFabLayout(
    context: Context, attrs: AttributeSet?
): CoordinatorLayout(context, attrs, 0) {

    private var fabPosition: Int = 0

    private enum class ExpandableFabPosition(val value: Int) {

        RIGHT(Gravity.BOTTOM or Gravity.END),

        LEFT(Gravity.BOTTOM or Gravity.START)
    }

    private enum class TitlePosition(val value: Int) {

        LEFT(Gravity.START or Gravity.CENTER_VERTICAL),

        RIGHT(Gravity.END or Gravity.CENTER_VERTICAL)
    }

    private val options: MutableList<Option> = mutableListOf()

    internal fun show(): Unit {
        throw NotImplementedError()
    }

    internal fun hide(): Unit {
        throw NotImplementedError()
    }

    private fun addOverlay(): Unit {
        throw NotImplementedError()
    }

    private fun addOptionFab(option: Option, index: Int, params: LayoutParams): Unit {

        Log.i("ExpandableFabLayout", "addOptionFab: $fabPosition")
        options.add(option)

        option.label?.let { label ->
            AppCompatTextView(context).apply {

                text = label

                addView(this)
                (layoutParams as LayoutParams).let {

                    it.anchorId = option.id
                    it.rightMargin = 100
                    it.anchorGravity = TitlePosition.LEFT.value
                    it.gravity = TitlePosition.LEFT.value

                    layoutParams = it
                }
            }
        }
        (option.layoutParams as LayoutParams).let {
            if (fabPosition > 0) {
                it.anchorId = options[fabPosition - 1].id
            }
            it.bottomMargin = 50
            it.anchorGravity = Gravity.TOP
            it.gravity = ExpandableFabPosition.RIGHT.value
            option.layoutParams = it
        }
        fabPosition++
    }

    private fun addExpandableFab(): Unit {
        throw NotImplementedError()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        if (child is Option) {
            addOptionFab(child, index, params as LayoutParams)
        }
    }
}