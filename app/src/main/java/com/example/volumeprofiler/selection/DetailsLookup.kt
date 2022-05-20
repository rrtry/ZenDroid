package com.example.volumeprofiler.selection

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider

class DetailsLookup <T> (private val recyclerView: RecyclerView): ItemDetailsLookup<T>() {

    @Suppress("unchecked_cast")
    override fun getItemDetails(event: MotionEvent): ItemDetails<T>? {
        val view: View? = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            val viewHolderInterface = recyclerView.getChildViewHolder(view) as ViewHolderItemDetailsProvider<T>
            return viewHolderInterface.getItemDetails()
        }
        return null
    }
}