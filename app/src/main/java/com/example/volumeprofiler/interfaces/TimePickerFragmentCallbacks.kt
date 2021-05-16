package com.example.volumeprofiler.interfaces

import java.time.LocalDateTime

interface TimePickerFragmentCallbacks {

    fun onTimeSelected(date: LocalDateTime)
}