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
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.*
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.PriorityInterruptionsSelectionDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictionsDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictionsDialog
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class InterruptionFilterFragment: Fragment() {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var _binding: ZenPreferencesFragmentBinding? = null
    private val binding: ZenPreferencesFragmentBinding get() = _binding!!

    private var job: Job? = null

    private fun onPriorityResult(bundle: Bundle): Unit {
        val priorityCategories: ArrayList<Int> = bundle.getIntegerArrayList(PRIORITY_CATEGORIES_KEY) as ArrayList<Int>
        viewModel.priorityCategories.value = priorityCategories
    }

    private fun onSuppressedEffectsResult(bundle: Bundle): Unit {
        val type: Int = bundle.getInt(EFFECTS_TYPE_KEY)
        val effects: ArrayList<Int> = bundle.getIntegerArrayList(EFFECTS_KEY) as ArrayList<Int>
        if (type == 0) {
            viewModel.screenOffVisualEffects.value = effects
        } else {
            viewModel.screenOnVisualEffects.value = effects
        }
    }

    private fun disableNestedScrolling(): Unit {
        val appBar: AppBarLayout = requireActivity().findViewById(R.id.app_bar)
        appBar.setExpanded(false, true)
        ViewCompat.setNestedScrollingEnabled(requireView().findViewById(R.id.rootElement), false)
    }

    override fun onStop() {
        super.onStop()
        job?.cancel()
        job = null
    }

    private fun startFavoriteContactsActivity(): Unit {
        val intent: Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.zen_preferences_fragment, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    private fun collectEventsFlow(): Unit {
        job = viewModel.fragmentEventsFlow.onEach {
            when (it) {
                EditProfileViewModel.Event.StartContactsActivity -> {
                    startFavoriteContactsActivity()
                }
                is EditProfileViewModel.Event.ShowPopupWindow -> {
                    showPopupWindow(it.category)
                }
                is EditProfileViewModel.Event.ShowDialogFragment -> {
                    showDialog(it.dialogType)
                }
                else -> {
                    Log.i("EditProfileFragment", "unknown event")
                }

            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectEventsFlow()
        disableNestedScrolling()
        fragmentManager?.setFragmentResultListener(PRIORITY_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            onPriorityResult(bundle)
        }
        fragmentManager?.setFragmentResultListener(EFFECTS_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            onSuppressedEffectsResult(bundle)
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun showConversationsPopupWindow(popupMenu: PopupMenu): Unit {
        popupMenu.inflate(R.menu.dnd_conversations)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.all_conversations -> {
                    viewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_ANYONE
                    true
                }
                R.id.priority_conversations -> {
                    viewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_IMPORTANT
                    true
                }
                R.id.none -> {
                    viewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_NONE
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun displayDialogWindow(fragment: DialogFragment): Unit {
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    private fun getFragmentInstance(type: EditProfileViewModel.DialogType): DialogFragment {
        return when (type) {
            EditProfileViewModel.DialogType.SUPPRESSED_EFFECTS_ON -> ScreenOnVisualRestrictionsDialog.newInstance(viewModel.screenOnVisualEffects.value!!)
            EditProfileViewModel.DialogType.SUPPRESSED_EFFECTS_OFF -> ScreenOffVisualRestrictionsDialog.newInstance(viewModel.screenOffVisualEffects.value!!)
            EditProfileViewModel.DialogType.PRIORITY -> PriorityInterruptionsSelectionDialog.newInstance(viewModel.priorityCategories.value!!)
            else -> ProfileNameInputDialog.newInstance(viewModel.title.value!!)
        }
    }

    private fun showDialog(type: EditProfileViewModel.DialogType): Unit {
        displayDialogWindow(getFragmentInstance(type))
    }

    private fun showExceptionsPopupWindow(popupMenu: PopupMenu, category: Int): Unit {
        popupMenu.inflate(R.menu.dnd_exceptions)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId != R.id.none) {
                if (!viewModel.containsPriorityCategory(PRIORITY_CATEGORY_CALLS) && category == PRIORITY_CATEGORY_CALLS) {
                    viewModel.addPriorityCategory(PRIORITY_CATEGORY_CALLS)
                } else if (!viewModel.containsPriorityCategory(PRIORITY_CATEGORY_MESSAGES) && category == PRIORITY_CATEGORY_MESSAGES) {
                    viewModel.addPriorityCategory(PRIORITY_CATEGORY_MESSAGES)
                }
            }
            when (it.itemId) {
                R.id.starred_contacts -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        viewModel.priorityMessageSenders.value = PRIORITY_SENDERS_STARRED
                    } else {
                        viewModel.priorityCallSenders.value = PRIORITY_SENDERS_STARRED
                    }
                    true
                }
                R.id.contacts -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        viewModel.priorityMessageSenders.value = PRIORITY_SENDERS_CONTACTS
                    } else {
                        viewModel.priorityCallSenders.value = PRIORITY_SENDERS_CONTACTS
                    }
                    true
                }
                R.id.anyone -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        viewModel.priorityMessageSenders.value = PRIORITY_SENDERS_ANY
                    } else {
                        viewModel.addPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
                        viewModel.priorityCallSenders.value = PRIORITY_SENDERS_ANY
                    }
                    true
                }
                R.id.none -> {
                    viewModel.removePriorityCategory(category)
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

    companion object {
        const val EFFECTS_REQUEST_KEY: String = "request_key"
        const val PRIORITY_REQUEST_KEY: String = "priority_request_key"
        const val PRIORITY_CATEGORIES_KEY: String = "priority_categories_key"
        const val EFFECTS_KEY: String = "effects_key"
        const val EFFECTS_TYPE_KEY: String = "effects_type_key"
    }
}