package com.example.volumeprofiler.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
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
import com.example.volumeprofiler.interfaces.ProfileActionListener
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.ui.BindingConverters.interruptionFilterToString
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment.Companion.SHARED_TRANSITION_PROFILE_IMAGE
import java.lang.ref.WeakReference
import java.util.*

class ProfileAdapter(
    private val activity: Activity,
    private val viewGroup: ViewGroup,
    listener: WeakReference<ProfilesListFragment>
): ListAdapter<Profile, ProfileAdapter.ProfileHolder>(object : DiffUtil.ItemCallback<Profile>() {

    override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem == newItem
    }

}), ListAdapterItemProvider<Profile> {

    private val profileActionListener = listener.get()!! as ProfileActionListener

    inner class ProfileHolder(private val binding: ProfileItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<Profile>,
        View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Profile> {
            return ItemDetails(bindingAdapterPosition, getItemAtPosition(bindingAdapterPosition))
        }

        private fun expand(animate: Boolean) {
            if (animate) {
                TransitionManager.beginDelayedTransition(viewGroup, AutoTransition())
                binding.expandButton.animate().rotation(180.0f).start()
            } else {
                binding.expandButton.rotation = 180f
            }
            binding.itemSeparator.visibility = View.VISIBLE
            binding.expandableView.visibility = View.VISIBLE
        }

        private fun collapse() {
            TransitionManager.beginDelayedTransition(viewGroup, AutoTransition())
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
                if (binding.expandableView.isVisible) collapse() else expand(true)
            }
            binding.editProfileButton.setOnClickListener {
                profileActionListener.onEdit(profile, createSceneTransitionAnimation(binding))
            }
            binding.removeProfileButton.setOnClickListener {
                profileActionListener.onRemove(profile)
            }
            profileActionListener.isEnabled(profile).let {
                binding.checkBox.isChecked = it
                if (it) {
                    profileActionListener.setSelection(profile.id)
                }
            }
            if (animate) {
                Animations.selected(itemView, isSelected)
            } else {
                setViewScale(isSelected)
            }
        }

        override fun onClick(v: View?) {
            profileActionListener.onEnable(
                getItemAtPosition(bindingAdapterPosition)
            )
        }
    }

    fun createSceneTransitionAnimation(binding: ProfileItemViewBinding): Bundle? {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity,
            androidx.core.util.Pair.create(binding.profileIcon, SHARED_TRANSITION_PROFILE_IMAGE)
        ).toBundle()
    }

    fun setSelection(profile: Profile?, currentSelection: UUID?) {
        profile?.id?.also {
            updatePreviousProfileView(currentSelection)
            updateCurrentProfileView(it)
            profileActionListener.setSelection(it)
            return
        }
        updatePreviousProfileView(currentSelection)
        profileActionListener.setSelection(null)
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
            profileActionListener.isSelected(getItem(position)),
            true)
    }

    override fun getItemKey(position: Int): Profile {
        return getItemAtPosition(position)
    }

    override fun getPosition(key: Profile): Int {
        return currentList.indexOfFirst { key == it }
    }
}