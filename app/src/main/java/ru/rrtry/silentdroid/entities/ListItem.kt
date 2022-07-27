package ru.rrtry.silentdroid.entities

interface ListItem<T> {

    val id: T
    val viewType: Int
}