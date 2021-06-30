package com.example.volumeprofiler.interfaces

import androidx.recyclerview.selection.ItemDetailsLookup
import java.util.*

interface ViewHolderDetails {

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<String>
}