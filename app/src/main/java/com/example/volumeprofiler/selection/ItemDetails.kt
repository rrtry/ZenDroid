package com.example.volumeprofiler.selection

import androidx.recyclerview.selection.ItemDetailsLookup

class ItemDetails<T> (private val adapterPosition: Int, private val selectionKey: T?): ItemDetailsLookup.ItemDetails<T>() {

    override fun getSelectionKey(): T? {
        return selectionKey
    }

    override fun getPosition(): Int {
        return adapterPosition
    }
}