package ru.rrtry.silentdroid.interfaces

interface ListAdapterItemProvider<T> {

    fun getItemKey(position: Int): T

    fun getPosition(key: T): Int
}