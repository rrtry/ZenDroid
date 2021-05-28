package com.example.volumeprofiler.interfaces

import java.util.ArrayList

interface DaysPickerDialogCallbacks {

    fun onDaysSelected(arrayList: ArrayList<Int>): Unit
}