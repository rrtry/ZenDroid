package ru.rrtry.silentdroid.interfaces

interface AdapterDatasetProvider<T> {

    var currentList: List<T>

    fun <I> getItem(position: Int): I = currentList[position] as I
}