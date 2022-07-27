package ru.rrtry.silentdroid.selection

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import ru.rrtry.silentdroid.interfaces.ViewHolderItemDetailsProvider

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