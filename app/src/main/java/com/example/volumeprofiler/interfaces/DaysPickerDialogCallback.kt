package com.example.volumeprofiler.interfaces

import java.util.ArrayList

interface DaysPickerDialogCallback {

    fun onDaysSelected(arrayList: ArrayList<Int>): Unit
}