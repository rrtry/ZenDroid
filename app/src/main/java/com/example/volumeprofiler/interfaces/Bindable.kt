package com.example.volumeprofiler.interfaces

interface Bindable<T> {

    fun bind(model: T): Unit
}