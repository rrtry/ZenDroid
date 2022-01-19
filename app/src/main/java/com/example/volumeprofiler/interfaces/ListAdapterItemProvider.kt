package com.example.volumeprofiler.interfaces

interface ListAdapterItemProvider <T> {

    fun getItemKey(position: Int): T

    fun getPosition(key: T): Int
}