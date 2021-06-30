package com.example.volumeprofiler.adapters

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.interfaces.ViewHolderDetails

class DetailsLookup (private val recyclerView: RecyclerView): ItemDetailsLookup<String>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
        val view: View? = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            val ViewHolderInterface: ViewHolderDetails = recyclerView.getChildViewHolder(view) as ViewHolderDetails
            return ViewHolderInterface.getItemDetails()
        }
        return null
    }
}