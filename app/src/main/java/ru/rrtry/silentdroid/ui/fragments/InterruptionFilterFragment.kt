package ru.rrtry.silentdroid.ui.fragments

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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.interfaces.ProfileDetailsActivityCallback
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.view.Gravity
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import ru.rrtry.silentdroid.databinding.ZenPreferencesFragmentBinding
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.DialogType.*

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class InterruptionFilterFragment: ViewBindingFragment<ZenPreferencesFragmentBinding>() {

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()

    @Inject
    lateinit var profileManager: ProfileManager

    private var callback: ProfileDetailsActivityCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as ProfileDetailsActivityCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransitionSet().apply {

            addTransition(Fade())
            addTransition(Slide(Gravity.END))

            enterTransition = this
            exitTransition = this
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewBinding.viewModel = detailsViewModel
        viewBinding.lifecycleOwner = viewLifecycleOwner
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callback?.setNestedScrollingEnabled(false)
        viewLifecycleOwner.lifecycleScope.launch {
            detailsViewModel.fragmentEventsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    StartContactsActivity -> startFavoriteContactsActivity()
                    is ShowPopupWindow -> showPopupWindow(it.category)
                    is ShowDialogFragment -> showDialog(it.dialogType)
                    else -> Log.i("EditProfileFragment", "unknown event")
                }
            }.collect()
        }
    }

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ZenPreferencesFragmentBinding {
        return DataBindingUtil.inflate(inflater, R.layout.zen_preferences_fragment, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    private fun getFragmentInstance(type: ProfileDetailsViewModel.DialogType): DialogFragment {
        return when (type) {
            SUPPRESSED_EFFECTS_ON -> SuppressedEffectsOnDialog.newInstance(detailsViewModel.getProfile())
            SUPPRESSED_EFFECTS_OFF -> SuppressedEffectsOffDialog.newInstance(detailsViewModel.getProfile())
            PRIORITY_CATEGORIES -> PriorityCategoriesDialog.newInstance(detailsViewModel.getProfile())
            else -> throw IllegalArgumentException("Unknown dialog type")
        }
    }

    private fun showDialog(type: ProfileDetailsViewModel.DialogType) {
        getFragmentInstance(type).show(requireActivity().supportFragmentManager, null)
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun showConversationsPopupWindow(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.dnd_conversations)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId != R.id.none) detailsViewModel.addPriorityCategory(PRIORITY_CATEGORY_CONVERSATIONS)
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
                    detailsViewModel.removePriorityCategory(PRIORITY_CATEGORY_CONVERSATIONS)
                    detailsViewModel.primaryConversationSenders.value = CONVERSATION_SENDERS_NONE
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showPopupWindow(category: Int) {
        val view: View? = when (category) {
            PRIORITY_CATEGORY_MESSAGES -> viewBinding.exceptionsMessagesLayout
            PRIORITY_CATEGORY_CALLS -> viewBinding.exceptionsCallsLayout
            PRIORITY_CATEGORY_CONVERSATIONS -> viewBinding.exceptionsConversationsLayout
            else -> null
        }
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        if (category == PRIORITY_CATEGORY_CONVERSATIONS) {
            if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
                showConversationsPopupWindow(popupMenu)
            }
        } else {
            showExceptionsPopupWindow(popupMenu, category)
        }
    }

    private fun showExceptionsPopupWindow(popupMenu: PopupMenu, category: Int) {
        popupMenu.inflate(R.menu.dnd_exceptions)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId != R.id.none) detailsViewModel.addPriorityCategory(category)
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

    private fun startFavoriteContactsActivity() {
        val intent: Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}