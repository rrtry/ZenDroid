package com.example.volumeprofiler.viewmodels

open class EventWrapper<out T>(private val content: T, private val numberOfObservers: Byte = 2) {

    var hasBeenHandled = false
        private set

    var timesObserved: Byte = 0
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            timesObserved++
            if (timesObserved == numberOfObservers) {
                hasBeenHandled = true
            }
            content
        }
    }

    fun peekContent(): T = content
}