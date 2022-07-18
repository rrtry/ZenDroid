package com.example.volumeprofiler.interfaces

interface AdapterDatasetProvider<T> {

    var currentList: List<T>

    fun <I> getItem(position: Int): I = currentList[position] as I
}