package com.example.volumeprofiler.views

import android.content.Context
import android.util.AttributeSet
import com.example.volumeprofiler.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Option(
    context: Context, attributeSet: AttributeSet?
): FloatingActionButton(context, attributeSet) {

    var label: String?

    init {
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.Option,
            0, 0).apply {
            try {
                label = getString(R.styleable.Option_label)
            } finally {
                recycle()
            }
        }
    }
}