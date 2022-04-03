package com.example.volumeprofiler.interfaces

interface DetailsViewContract <T> {

    fun onUpdate(item: T): Unit

    fun onInsert(item: T): Unit

    fun onCancel(): Unit
}