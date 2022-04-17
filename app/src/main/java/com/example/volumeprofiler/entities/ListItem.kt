package com.example.volumeprofiler.entities

sealed class ListItem {

    abstract val id: Long
    abstract val itemViewType: Int
}
