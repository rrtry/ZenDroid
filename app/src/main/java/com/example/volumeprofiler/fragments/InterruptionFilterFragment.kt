package com.example.volumeprofiler.fragments

import android.provider.Settings.*
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
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest.permission.*
import android.content.Context
import com.example.volumeprofiler.activities.ProfileDetailsActivity
import com.example.volumeprofiler.util.ui.animations.AnimUtil

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class InterruptionFilterFragment: Fragment() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()

    private var _binding: ZenPreferencesFragmentBinding? = null
    private val binding: ZenPreferencesFragmentBinding get() = _binding!!

    private var callback: EditProfileActivityCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as EditProfileActivityCallbacks
        detailsViewModel.currentFragmentTag.value = ProfileDetailsActivity.TAG_INTERRUPTIONS_FRAGMENT
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        _binding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.zen_preferences_fragment, container, false)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectEventsFlow()
        disableNestedScrolling()
        hideToolbarItems()

        requireActivity().supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, viewLifecycleOwner,
            { _, result ->
                if (result.getString(PermissionExplanationDialog.EXTRA_PERMISSION) == ACCESS_NOTIFICATION_POLICY) {
                    if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                        startNotificationPolicySettingsActivity()
                    }
                    else {
                        callback?.onFragmentReplace(ProfileDetailsActivity.PROFILE_DETAILS_FRAGMENT)
                    }
                }
            })
        requireActivity().supportFragmentManager.setFragmentResultListener(PRIORITY_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            onPriorityResult(bundle)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(EFFECTS_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            onSuppressedEffectsResult(bundle)
        }
    }

    private fun collectEventsFlow(): Unit {
        viewLifecycleOwner.lifecycleScope.launch {
            detailsViewModel.fragmentEventsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    ProfileDetailsViewModel.Event.StartContactsActivity -> {
                        startFavoriteContactsActivity()
                    }
                    is ProfileDetailsViewModel.Event.ShowPopupWindow -> {
                        showPopupWindow(it.category)
                    }
                    is ProfileDetailsViewModel.Event.ShowDialogFragment -> {
                        showDialog(it.dialogType)
                    }
                    else -> {
                        Log.i("EditProfileFragment", "unknown event")
                    }
                }
            }.collect()
        }
    }

    private fun hideToolbarItems(): Unit {
        val binding = callback?.getBinding()!!
        if (binding.menuEditNameButton.visibility != View.INVISIBLE) {
            AnimUtil.scaleAnimation(binding.menuEditNameButton, false)
        }
        if (binding.menuSaveChangesButton.visibility != View.INVISIBLE) {
            AnimUtil.scaleAnimation(binding.menuSaveChangesButton, false)
        }
    }

    private fun disableNestedScrolling(): Unit {
        val activityBinding = callback?.getBinding()!!
        activityBinding.appBar.setExpanded(false, true)
        ViewCompat.setNestedScrollingEnabled(binding.rootElement, false)
    }

    private fun getFragmentInstance(type: ProfileDetailsViewModel.DialogType): DialogFragment {
        return when (type) {
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_ON -> SuppressedEffectsOnDialog.newInstance(detailsViewModel.getProfile())
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_OFF -> SuppressedEffectsOffDialog.newInstance(detailsViewModel.getProfile())
            ProfileDetailsViewModel.DialogType.PRIORITY -> PriorityCategoriesDialog.newInstance(detailsViewModel.getProfile())
            else -> ProfileNameInputDialog.newInstance(detailsViewModel.title.value)
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

    private fun startNotificationPolicySettingsActivity(): Unit {
        val intent: Intent = Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun onPriorityResult(bundle: Bundle): Unit {
        detailsViewModel.priorityCategories.value = bundle.getInt(BaseDialog.EXTRA_MASK)
    }

    private fun onSuppressedEffectsResult(bundle: Bundle): Unit {
        val effectsMask: Int = bundle.getInt(BaseDialog.EXTRA_MASK)
        detailsViewModel.suppressedVisualEffects.value = effectsMask
    }

    companion object {

        const val EFFECTS_REQUEST_KEY: String = "request_key"
        const val PRIORITY_REQUEST_KEY: String = "priority_request_key"
    }
}