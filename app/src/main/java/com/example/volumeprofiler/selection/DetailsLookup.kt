package com.example.volumeprofiler.selection

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider

class DetailsLookup<T> (private val recyclerView: RecyclerView): ItemDetailsLookup<T>() {

    @Suppress("unchecked_cast")
    override fun getItemDetails(event: MotionEvent): ItemDetails<T>? {
        recyclerView.findChildViewUnder(event.x, event.y)?.let {
            return (recyclerView.getChildViewHolder(it) as ViewHolderItemDetailsProvider<T>)
                .getItemDetails()
        }
        return null
    }
}