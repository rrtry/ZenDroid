package ru.rrtry.silentdroid.interfaces

interface DetailsViewContract <T> {

    fun onUpdate(item: T)

    fun onInsert(item: T)

    fun onFinish(result: Boolean)
}