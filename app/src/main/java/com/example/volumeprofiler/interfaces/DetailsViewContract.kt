package com.example.volumeprofiler.interfaces

interface DetailsViewContract <T> {

    fun onUpdate(item: T)

    fun onInsert(item: T)

    fun onCancel()
}