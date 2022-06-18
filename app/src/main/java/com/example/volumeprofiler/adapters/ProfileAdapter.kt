package com.example.volumeprofiler.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
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
import com.example.volumeprofiler.interfaces.ViewHolder
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.Animations.selected
import com.example.volumeprofiler.ui.BindingConverters.interruptionFilterToString
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment.Companion.SHARED_TRANSITION_PROFILE_IMAGE
import java.lang.ref.WeakReference
import java.util.*

class ProfileAdapter(
    var currentList: List<Profile>,
    private val container: ViewGroup,
    listener: WeakReference<ProfilesListFragment>
): RecyclerView.Adapter<ProfileAdapter.ProfileHolder>(), ListAdapterItemProvider<Profile> {

    private val profileActionListener = listener.get()!! as ProfileActionListener

    inner class ProfileHolder(override val binding: ProfileItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<Profile>,
        View.OnClickListener,
        ViewHolder<ProfileItemViewBinding> {

        init {
            binding.root.setOnClickListener(this)
            binding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Profile> {
            return ItemDetails(bindingAdapterPosition, currentList[bindingAdapterPosition])
        }

        private fun expand(animate: Boolean) {
            if (animate) {
                TransitionManager.beginDelayedTransition(container, AutoTransition())
                binding.expandButton.animate().rotation(180.0f).start()
            } else {
                binding.expandButton.rotation = 180f
            }
            binding.itemSeparator.visibility = View.VISIBLE
            binding.expandableView.visibility = View.VISIBLE
        }

        private fun collapse() {
            TransitionManager.beginDelayedTransition(container, AutoTransition())
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

            if (animate) selected(binding.root, isSelected) else setViewScale(isSelected)

            binding.profileTitle.text = profile.title
            binding.interruptionFilter.text = interruptionFilterToString(profile.interruptionFilter)
            binding.profileIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, profile.iconRes))

            binding.expandButton.setOnClickListener {
                if (binding.expandableView.isVisible) collapse() else expand(true)
            }
            binding.editProfileButton.setOnClickListener {
                profileActionListener.onEditWithTransition(
                    profile,
                    binding.root,
                    Pair(binding.profileIcon, SHARED_TRANSITION_PROFILE_IMAGE)
                )
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
            binding.profileIcon.post {
                profileActionListener.onSharedViewReady()
            }
        }

        override fun onClick(v: View?) {
            profileActionListener.onEnable(
                currentList[bindingAdapterPosition]
            )
        }
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].id.mostSignificantBits and Long.MAX_VALUE
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
            currentList[position],
            profileActionListener.isSelected(currentList[position]),
            true)
    }

    override fun getItemKey(position: Int): Profile {
        return currentList[position]
    }

    override fun getPosition(key: Profile): Int {
        return currentList.indexOfFirst { key == it }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}