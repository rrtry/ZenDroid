package com.example.volumeprofiler.interfaces

interface ListViewContract <T> {

    fun updateAdapter(items: List<T>)

}