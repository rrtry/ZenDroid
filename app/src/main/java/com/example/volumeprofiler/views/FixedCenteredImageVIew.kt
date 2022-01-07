package com.example.volumeprofiler.views

import android.content.Context
import android.util.AttributeSet
import kotlin.math.ceil


class FixedCenterCrop(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if (drawable != null) {

            var height = MeasureSpec.getSize(heightMeasureSpec)
            var width = MeasureSpec.getSize(widthMeasureSpec)
            if (width >= height) height =
                ceil((width * drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth).toDouble())
                    .toInt() else width =
                ceil((height * drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight).toDouble())
                    .toInt()
            this.setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}