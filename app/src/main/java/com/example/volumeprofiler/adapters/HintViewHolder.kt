package com.example.volumeprofiler.adapters

import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.PowerSaveModeHintBinding
import com.example.volumeprofiler.entities.Hint
import com.example.volumeprofiler.entities.ListItem
import com.example.volumeprofiler.interfaces.AdapterDatasetProvider
import com.example.volumeprofiler.interfaces.ViewHolder
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails

class HintViewHolder(
    override val binding: PowerSaveModeHintBinding,
    private val datasetProvider: AdapterDatasetProvider<ListItem<Int>>
):  RecyclerView.ViewHolder(binding.root),
    ViewHolderItemDetailsProvider<Hint>,
    ViewHolder<PowerSaveModeHintBinding>
{
    override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Hint> {
        return ItemDetails(
            bindingAdapterPosition,
            datasetProvider.getItem<Hint>(bindingAdapterPosition)
        )
    }

    fun bind(hint: Hint) { binding.hint.text = hint.text }
}