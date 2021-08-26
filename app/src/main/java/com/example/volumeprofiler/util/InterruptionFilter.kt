package com.example.volumeprofiler.util

enum class InterruptionFilter(name: String) {

    PRIORITY("Priority only"),
    ALARMS("Alarms only"),
    TOTAL_SILENCE("Total silence"),
    NONE("None")
}