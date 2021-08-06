package com.example.volumeprofiler.interfaces

interface PriorityInterruptionsCallback {

    fun onPrioritySelected(categories: ArrayList<Int>): Unit
}