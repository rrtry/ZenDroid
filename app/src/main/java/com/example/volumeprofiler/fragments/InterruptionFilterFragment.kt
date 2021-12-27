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
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.PriorityInterruptionsSelectionDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictionsDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictionsDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest.permission.*
import com.example.volumeprofiler.activities.ProfileDetailsActivity

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class InterruptionFilterFragment: Fragment() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()

    private var _binding: ZenPreferencesFragmentBinding? = null
    private val binding: ZenPreferencesFragmentBinding get() = _binding!!

    private var callback: EditProfileActivityCallbacks? = null

    private fun disableNestedScrolling(): Unit {
        val appBar: AppBarLayout = requireActivity().findViewById(R.id.app_bar)
        appBar.setExpanded(false, true)
        ViewCompat.setNestedScrollingEnabled(binding.rootElement, false)
    }

    private fun startFavoriteContactsActivity(): Unit {
        val intent: Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.zen_preferences_fragment, container, false)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callback = requireActivity() as EditProfileActivityCallbacks
        collectEventsFlow()
        disableNestedScrolling()

        requireActivity().supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, viewLifecycleOwner,
            { requestKey, result ->
                if (result.getString(PermissionExplanationDialog.EXTRA_PERMISSION) == ACCESS_NOTIFICATION_POLICY) {
                    if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                        val intent: Intent = Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                    else {
                        callback?.onFragmentReplace(ProfileDetailsActivity.PROFILE_FRAGMENT)
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

    override fun onDestroyView() {
        callback = null
        super.onDestroyView()
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

    private fun displayDialogWindow(fragment: DialogFragment): Unit {
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    private fun getFragmentInstance(type: ProfileDetailsViewModel.DialogType): DialogFragment {
        return when (type) {
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_ON -> ScreenOnVisualRestrictionsDialog.newInstance(
                ArrayList(detailsViewModel.screenOnVisualEffects.value)
            )
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_OFF -> ScreenOffVisualRestrictionsDialog.newInstance(
                ArrayList(detailsViewModel.screenOffVisualEffects.value)
            )
            ProfileDetailsViewModel.DialogType.PRIORITY -> PriorityInterruptionsSelectionDialog.newInstance(
                ArrayList(detailsViewModel.priorityCategories.value)
            )
            else -> ProfileNameInputDialog.newInstance(detailsViewModel.title.value)
        }
    }

    private fun showDialog(type: ProfileDetailsViewModel.DialogType): Unit {
        displayDialogWindow(getFragmentInstance(type))
    }

    private fun showExceptionsPopupWindow(popupMenu: PopupMenu, category: Int): Unit {
        popupMenu.inflate(R.menu.dnd_exceptions)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId != R.id.none) {
                if (!detailsViewModel.containsPriorityCategory(PRIORITY_CATEGORY_CALLS) && category == PRIORITY_CATEGORY_CALLS) {
                    detailsViewModel.addPriorityCategory(PRIORITY_CATEGORY_CALLS)
                } else if (!detailsViewModel.containsPriorityCategory(PRIORITY_CATEGORY_MESSAGES) && category == PRIORITY_CATEGORY_MESSAGES) {
                    detailsViewModel.addPriorityCategory(PRIORITY_CATEGORY_MESSAGES)
                }
            }
            when (it.itemId) {
                R.id.starred_contacts -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        detailsViewModel.priorityMessageSenders.value = PRIORITY_SENDERS_STARRED
                    } else {
                        detailsViewModel.priorityCallSenders.value = PRIORITY_SENDERS_STARRED
                    }
                    true
                }
                R.id.contacts -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        detailsViewModel.priorityMessageSenders.value = PRIORITY_SENDERS_CONTACTS
                    } else {
                        detailsViewModel.priorityCallSenders.value = PRIORITY_SENDERS_CONTACTS
                    }
                    true
                }
                R.id.anyone -> {
                    if (category == PRIORITY_CATEGORY_MESSAGES) {
                        detailsViewModel.priorityMessageSenders.value = PRIORITY_SENDERS_ANY
                    } else {
                        detailsViewModel.addPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
                        detailsViewModel.priorityCallSenders.value = PRIORITY_SENDERS_ANY
                    }
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

    override fun onResume() {
        super.onResume()
        if (!profileUtil.isNotificationPolicyAccessGranted()) {
            ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
        }
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

    private fun onPriorityResult(bundle: Bundle): Unit {
        val priorityCategories: ArrayList<Int> = bundle.getIntegerArrayList(PRIORITY_CATEGORIES_KEY) as ArrayList<Int>
        detailsViewModel.priorityCategories.value = priorityCategories
    }

    private fun onSuppressedEffectsResult(bundle: Bundle): Unit {
        val type: Int = bundle.getInt(EFFECTS_TYPE_KEY)
        val effects: ArrayList<Int> = bundle.getIntegerArrayList(EFFECTS_KEY) as ArrayList<Int>
        if (type == 0) {
            detailsViewModel.screenOffVisualEffects.value = effects
        } else {
            detailsViewModel.screenOnVisualEffects.value = effects
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