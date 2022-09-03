package ru.rrtry.silentdroid.ui.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class ViewBindingActivity <T: ViewBinding>: AppCompatActivity() {

    private var viewBindingImpl: T? = null
    protected val viewBinding: T get() = viewBindingImpl!!

    abstract fun getBinding(): T
    abstract fun setWindowTransitions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBindingImpl = getBinding()
        setWindowTransitions()
        setContentView(viewBinding.root)

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBindingImpl = null
    }

    abstract fun onBack()
}