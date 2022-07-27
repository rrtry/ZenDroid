package ru.rrtry.silentdroid.adapters

import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import ru.rrtry.silentdroid.databinding.PowerSaveModeHintBinding
import ru.rrtry.silentdroid.entities.Hint
import ru.rrtry.silentdroid.entities.ListItem
import ru.rrtry.silentdroid.interfaces.AdapterDatasetProvider
import ru.rrtry.silentdroid.interfaces.ViewHolder
import ru.rrtry.silentdroid.interfaces.ViewHolderItemDetailsProvider
import ru.rrtry.silentdroid.selection.ItemDetails

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