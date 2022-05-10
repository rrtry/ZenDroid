package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.*
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class InterruptionFilterFragment: Fragment() {

    @Inject
    lateinit var profileManager: ProfileManager

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()

    private var bindingImpl: ZenPreferencesFragmentBinding? = null
    private val binding: ZenPreferencesFragmentBinding get() = bindingImpl!!

    private var callback: EditProfileActivityCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        bindingImpl = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindingImpl = DataBindingUtil.inflate(inflater, R.layout.zen_preferences_fragment, container, false)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setNestedScrollingEnabled(binding.scrollView, false)

        viewLifecycleOwner.lifecycleScope.launch {
            detailsViewModel.fragmentEventsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    ProfileDetailsViewModel.ViewEvent.StartContactsActivity -> {
                        startFavoriteContactsActivity()
                    }
                    is ProfileDetailsViewModel.ViewEvent.ShowPopupWindow -> {
                        showPopupWindow(it.category)
                    }
                    is ProfileDetailsViewModel.ViewEvent.ShowDialogFragment -> {
                        showDialog(it.dialogType)
                    }
                    else -> {
                        Log.i("EditProfileFragment", "unknown event")
                    }
                }
            }.collect()
        }
    }

    private fun getFragmentInstance(type: ProfileDetailsViewModel.DialogType): DialogFragment {
        return when (type) {
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_ON -> SuppressedEffectsOnDialog.newInstance(detailsViewModel.getProfile())
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_OFF -> SuppressedEffectsOffDialog.newInstance(detailsViewModel.getProfile())
            ProfileDetailsViewModel.DialogType.PRIORITY -> PriorityCategoriesDialog.newInstance(detailsViewModel.getProfile())
            else -> throw IllegalArgumentException("Unknown dialog type")
        }
    }

    private fun showDialog(type: ProfileDetailsViewModel.DialogType): Unit {
        getFragmentInstance(type).show(requireActivity().supportFragmentManager, null)
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun showConversationsPopupWindow(popupMenu: PopupMenu): Unit {
        popupMenu.inflate(R.menu.dnd_conversations)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.all_conversations -> {
                    detailsViewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_ANYONE
                    true
                }
                R.id.priority_conversations -> {
                    detailsViewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_IMPORTANT
                    true
                }
                R.id.none -> {
                    detailsViewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_NONE
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showPopupWindow(category: Int): Unit {
        val view: View? = when (category) {
            PRIORITY_CATEGORY_MESSAGES -> binding.exceptionsMessagesLayout
            PRIORITY_CATEGORY_CALLS -> binding.exceptionsCallsLayout
            PRIORITY_CATEGORY_CONVERSATIONS -> binding.exceptionsConversationsLayout
            else -> null
        }
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        if (category == PRIORITY_CATEGORY_CONVERSATIONS) {
            if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
                showConversationsPopupWindow(popupMenu)
            }
        }
        else {
            showExceptionsPopupWindow(popupMenu, category)
        }
    }

    private fun showExceptionsPopupWindow(popupMenu: PopupMenu, category: Int): Unit {
        popupMenu.inflate(R.menu.dnd_exceptions)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId != R.id.none) {
                detailsViewModel.addPriorityCategory(category)
            }
            when (it.itemId) {
                R.id.anyone -> {
                    detailsViewModel.setAllowedSenders(category, PRIORITY_SENDERS_ANY)
                    true
                }
                R.id.starred_contacts -> {
                    detailsViewModel.setAllowedSenders(category, PRIORITY_SENDERS_STARRED)
                    true
                }
                R.id.contacts -> {
                    detailsViewModel.setAllowedSenders(category, PRIORITY_SENDERS_CONTACTS)
                    true
                }
                R.id.none -> {
                    detailsViewModel.removePriorityCategory(category)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun startFavoriteContactsActivity(): Unit {
        val intent: Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}