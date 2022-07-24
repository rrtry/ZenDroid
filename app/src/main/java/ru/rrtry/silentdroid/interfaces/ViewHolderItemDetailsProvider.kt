package ru.rrtry.silentdroid.interfaces

import androidx.recyclerview.selection.ItemDetailsLookup

interface ViewHolderItemDetailsProvider <T> {

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<T>
}