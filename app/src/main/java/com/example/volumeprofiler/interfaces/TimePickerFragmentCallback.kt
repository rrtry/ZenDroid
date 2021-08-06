package com.example.volumeprofiler.interfaces

import java.time.LocalDateTime

interface TimePickerFragmentCallback {

    fun onTimeSelected(date: LocalDateTime)
}