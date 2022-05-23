package com.example.volumeprofiler.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.SelectableListItemInteractionListener
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.ui.BindingConverters.interruptionFilterToString
import java.lang.ref.WeakReference
import java.util.*

class ProfileAdapter(
    private val recyclerView: RecyclerView,
    listener: WeakReference<SelectableListItemInteractionListener<Profile, UUID>>
): ListAdapter<Profile, ProfileAdapter.ProfileHolder>(object : DiffUtil.ItemCallback<Profile>() {

    override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem == newItem
    }

}), ListAdapterItemProvider<String> {

    private val listener: SelectableListItemInteractionListener<Profile, UUID> = listener.get()!!

    inner class ProfileHolder(private val binding: ProfileItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<String>,
        View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(bindingAdapterPosition, getItemAtPosition(bindingAdapterPosition).id.toString())
        }

        private fun expand(animate: Boolean) {
            if (animate) {
                TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
                binding.expandButton.animate().rotation(180.0f).start()
            } else {
                binding.expandButton.rotation = 180f
            }
            binding.itemSeparator.visibility = View.VISIBLE
            binding.expandableView.visibility = View.VISIBLE
        }

        private fun collapse() {
            TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
            binding.itemSeparator.visibility = View.GONE
            binding.expandableView.visibility = View.GONE
            binding.expandButton.animate().rotation(0f).start()
        }

        private fun setViewScale(isSelected: Boolean) {
            val scale: Float = if (isSelected) 0.8f else 1.0f
            itemView.scaleX = scale
            itemView.scaleY = scale
        }

        fun bind(profile: Profile, isSelected: Boolean, animate: Boolean) {

            binding.profileTitle.text = profile.title
            binding.interruptionFilter.text = interruptionFilterToString(profile.interruptionFilter)
            binding.profileIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, profile.iconRes))

            binding.expandButton.setOnClickListener {
                if (binding.expandableView.isVisible) {
                    collapse()
                } else {
                    expand(true)
                }
            }
            binding.editProfileButton.setOnClickListener {
                listener.onEdit(profile, binding)
            }
            binding.removeProfileButton.setOnClickListener {
                listener.onRemove(profile)
            }
            listener.isEnabled(profile).let {
                binding.checkBox.isChecked = it
                if (it) {
                    listener.setSelection(profile.id)
                }
            }
            if (animate) {
                Animations.selected(itemView, isSelected)
            } else {
                setViewScale(isSelected)
            }
        }

        override fun onClick(v: View?) {
            getItemAtPosition(bindingAdapterPosition).also {
                listener.onEnable(it)
            }
        }
    }

    fun setSelection(profile: Profile?, currentSelection: UUID?) {
        profile?.id?.also {
            updatePreviousProfileView(currentSelection)
            updateCurrentProfileView(it)
            listener.setSelection(it)
            return
        }
        updatePreviousProfileView(currentSelection)
        listener.setSelection(null)
    }

    private fun updatePreviousProfileView(currentSelection: UUID?) {
        currentSelection?.let {
            notifyItemChanged(getPosition(it), true)
        }
    }

    private fun updateCurrentProfileView(uuid: UUID) {
        notifyItemChanged(getPosition(uuid), true)
    }

    private fun getPosition(id: UUID): Int {
        return currentList.indexOfFirst {
            it.id == id
        }
    }

    fun getItemAtPosition(position: Int): Profile {
        return getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        return ProfileHolder(
            ProfileItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProfileHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.bind(
            getItem(position),
            listener.isSelected(getItem(position)),
            true)
    }

    override fun getItemKey(position: Int): String {
        return getItemAtPosition(position).id.toString()
    }

    override fun getPosition(key: String): Int {
        return currentList.indexOfFirst { key == it.id.toString() }
    }
}