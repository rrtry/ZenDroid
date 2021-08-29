package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager.Policy.*
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.*
import com.example.volumeprofiler.fragments.dialogs.multiChoice.PriorityInterruptionsSelectionDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictionsDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictionsDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.interfaces.PriorityInterruptionsCallback
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.example.volumeprofiler.util.animations.AnimUtil.Companion.scaleAnimation

@SuppressLint("UseSwitchCompatOrMaterialCode")
class ZenModePreferencesFragment: Fragment(), PriorityInterruptionsCallback, VisualRestrictionsCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null

    private var _binding: ZenPreferencesFragmentBinding? = null
    private val binding: ZenPreferencesFragmentBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun disableNestedScrolling(): Unit {
        val appBar: AppBarLayout = requireActivity().findViewById(R.id.app_bar)
        appBar.setExpanded(false, false)
        ViewCompat.setNestedScrollingEnabled(requireView().findViewById(R.id.rootElement), false)
    }

    private fun hideMenuOptions(): Unit {
        val activity: Activity = requireActivity()
        val saveButton: ImageButton = activity.findViewById(R.id.menuSaveChangesButton)
        val editNameButton: ImageButton = activity.findViewById(R.id.menuEditNameButton)
        scaleAnimation(saveButton, false)
        if (editNameButton.visibility == View.VISIBLE) {
            scaleAnimation(editNameButton, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        disableNestedScrolling()
        setToolbar()
        updateUI()
    }

    private fun setToolbar(): Unit {
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        setToolbarTitle()
        setActionBarNavigationButton()
        hideMenuOptions()
    }

    private fun setActionBarNavigationButton(): Unit {
        val activity: AppCompatActivity = requireActivity() as AppCompatActivity
        val shouldDisplayNavArrow: Boolean = activity.supportFragmentManager.backStackEntryCount > 0
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(shouldDisplayNavArrow)
    }

    private fun setToolbarTitle(): Unit {
        val activity: Activity = requireActivity()
        val toolbarLayout: CollapsingToolbarLayout = activity.findViewById(R.id.toolbar_layout)
        toolbarLayout.title = "Do not disturb preferences"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId: Int = item.itemId
        if (itemId == android.R.id.home) {
            callbacks?.onPopBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = ZenPreferencesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initViews(view: View): Unit {
        val onClickListener = View.OnClickListener {
            when (it.id) {
                binding.blockWhenScreenIsOnLayout?.id -> {
                    if (binding.blockWhenScreenIsOnSwitch != null) {
                        if (binding.blockWhenScreenIsOnSwitch!!.isChecked) {
                            binding.blockWhenScreenIsOnSwitch!!.isChecked = false
                            viewModel.profile!!.screenOnVisualEffects.remove(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                        else {
                            binding.blockWhenScreenIsOnSwitch!!.isChecked = true
                            viewModel.profile!!.screenOffVisualEffects.add(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                    }
                }
                binding.blockWhenScreenIsOffLayout?.id -> {
                    if (binding.blockWhenScreenIsOffSwitch != null) {
                        if (binding.blockWhenScreenIsOffSwitch!!.isChecked) {
                            binding.blockWhenScreenIsOffSwitch!!.isChecked = false
                            viewModel.profile!!.screenOffVisualEffects.remove(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                        else {
                            binding.blockWhenScreenIsOffSwitch!!.isChecked = true
                            viewModel.profile!!.screenOffVisualEffects.add(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                    }
                }
                binding.exceptionsCallsLayout.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_CALLS, it)
                }
                binding.exceptionsMessagesLayout.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_MESSAGES, it)
                }
                binding.exceptionsConversationsLayout?.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_MESSAGES, it)
                }
                binding.repeatCallersLayout.id -> {
                    if (binding.repeatCallersSwitch.isChecked) {
                        binding.repeatCallersSwitch.isChecked = false
                        viewModel.profile!!.priorityCategories.remove(PRIORITY_CATEGORY_REPEAT_CALLERS)
                    }
                    else {
                        binding.repeatCallersSwitch.isChecked = true
                        viewModel.profile!!.priorityCategories.add(PRIORITY_CATEGORY_REPEAT_CALLERS)
                    }
                }
                binding.otherInterruptionsLayout.id -> {
                    val fragment = PriorityInterruptionsSelectionDialog.newInstance(viewModel.profile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, OTHER_INTERRUPTIONS_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                binding.whenScreenIsOffLayout?.id -> {
                    val fragment = ScreenOffVisualRestrictionsDialog.newInstance(viewModel.profile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, WHEN_SCREEN_IS_OFF_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                binding.whenScreenIsOnLayout?.id -> {
                    val fragment = ScreenOnVisualRestrictionsDialog.newInstance(viewModel.profile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, WHEN_SCREEN_IS_ON_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
        }
        binding.blockWhenScreenIsOffLayout?.setOnClickListener(onClickListener)
        binding.blockWhenScreenIsOnLayout?.setOnClickListener(onClickListener)
        binding.exceptionsCallsLayout.setOnClickListener(onClickListener)
        binding.repeatCallersLayout.setOnClickListener(onClickListener)
        binding.exceptionsMessagesLayout.setOnClickListener(onClickListener)
        binding.exceptionsConversationsLayout?.setOnClickListener(onClickListener)
        binding.otherInterruptionsLayout.setOnClickListener(onClickListener)
        binding.whenScreenIsOffLayout?.setOnClickListener(onClickListener)
        binding.whenScreenIsOnLayout?.setOnClickListener(onClickListener)
    }

    private fun updateUI(): Unit {
        binding.otherInterruptionsDescription.text = getPriorityCategoriesDescription(viewModel.profile!!.priorityCategories)
        binding.exceptionsCallsDescription.text = if (!containsCategory(PRIORITY_CATEGORY_CALLS)) "None" else when (viewModel.profile!!.priorityCallSenders) {
            PRIORITY_SENDERS_ANY -> {
                "From anyone"
            }
            PRIORITY_SENDERS_CONTACTS -> {
                "From contacts only"
            }
            PRIORITY_SENDERS_STARRED -> {
                "From starred contacts only"
            }
            else -> "None"
        }
        binding.exceptionsMessagesDescription.text = if (!containsCategory(PRIORITY_CATEGORY_MESSAGES)) "None" else when (viewModel.profile!!.priorityMessageSenders) {
            PRIORITY_SENDERS_ANY -> {
                "From anyone"
            }
            PRIORITY_SENDERS_CONTACTS -> {
                "From contacts only"
            }
            PRIORITY_SENDERS_STARRED -> {
                "From starred contacts only"
            }
            else -> "None"
        }
        binding.exceptionsMessagesDescription.text = when (viewModel.profile!!.primaryConversationSenders) {
            CONVERSATION_SENDERS_ANYONE -> {
                "From anyone"
            }
            CONVERSATION_SENDERS_IMPORTANT -> {
                "From important conversations only"
            }
            CONVERSATION_SENDERS_NONE -> {
                "None"
            }
            else -> "None"
        }
        binding.whenScreenIsOnDescription?.text = getVisualEffectsDescription(viewModel.profile!!.screenOnVisualEffects, 1)
        binding.whenScreenIsOffDescription?.text = getVisualEffectsDescription(viewModel.profile!!.screenOffVisualEffects, 0)
    }

    private fun getPriorityCategoriesDescription(list: List<Int>): String {
        if (list.isEmpty() || (list.size <= 2 && list.contains(PRIORITY_CATEGORY_CALLS) || list.contains(PRIORITY_CATEGORY_MESSAGES))) {
            return "None"
        }
        val categoryMap: ArrayMap<Int, String> =
                arrayMapOf(PRIORITY_CATEGORY_ALARMS to "alarms, ",
                           PRIORITY_CATEGORY_MEDIA to "media, ",
                           PRIORITY_CATEGORY_SYSTEM to "touch sounds, ",
                           PRIORITY_CATEGORY_REMINDERS to "reminders, ",
                           PRIORITY_CATEGORY_EVENTS to "events, ")
        val stringBuilder: StringBuilder = StringBuilder()
        stringBuilder.append("Allow ")
        for (i in list) {
            val value: String? = categoryMap[i]
            if (value != null) {
                stringBuilder.append(value)
            }
        }
        for (i in 1..2) {
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        }
        return stringBuilder.toString()
    }

    private fun containsCategory(category: Int): Boolean {
        val list: List<Int> = viewModel.profile!!.priorityCategories
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (category == PRIORITY_CATEGORY_ALARMS || category == PRIORITY_CATEGORY_MEDIA || category == PRIORITY_CATEGORY_SYSTEM)) {
            list.contains(category)
        }
        else {
            list.contains(category)
        }
    }

    private fun getVisualEffectsDescription(list: List<Int>, type: Int): String {
        if (list.isEmpty()) {
            return "None"
        }
        val effectsMap: ArrayMap<Int, String> = if (type == 0) arrayMapOf(
                SUPPRESSED_EFFECT_LIGHTS to "Don\'t blink notification light, ",
                SUPPRESSED_EFFECT_FULL_SCREEN_INTENT to "Don\'t turn on screen, ",
                SUPPRESSED_EFFECT_AMBIENT to "Don\'t wake for notifications, "
        ) else arrayMapOf(
                SUPPRESSED_EFFECT_BADGE to "Hide notification dots, ",
                SUPPRESSED_EFFECT_STATUS_BAR to "Hide status bar icons, ",
                SUPPRESSED_EFFECT_PEEK to "Don\'t pop notifications on screen, ",
                SUPPRESSED_EFFECT_NOTIFICATION_LIST to "Hide from notifications list, "
        )
        val stringBuilder: StringBuilder = StringBuilder()
        for (i in list) {
            val value: String? = effectsMap[i]
            if (value != null) {
                stringBuilder.append(value)
            }
        }
        for (i in 1..2) {
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        }
        return stringBuilder.toString()
    }

    private fun showPopupMenu(category: Int, view: View): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        if (category == PRIORITY_CATEGORY_CONVERSATIONS) {
            popupMenu.inflate(R.menu.dnd_conversations)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.all_conversations -> {
                        binding.exceptionsConversationsDescirption?.text = "Anyone"
                        viewModel.profile!!.primaryConversationSenders = CONVERSATION_SENDERS_ANYONE
                        true
                    }
                    R.id.priority_conversations -> {
                        binding.exceptionsConversationsDescirption?.text = "Important"
                        viewModel.profile!!.primaryConversationSenders = CONVERSATION_SENDERS_IMPORTANT
                        true
                    }
                    R.id.none -> {
                        binding.exceptionsConversationsDescirption?.text = "None"
                        viewModel.profile!!.primaryConversationSenders = CONVERSATION_SENDERS_NONE
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            popupMenu.show()
        }
        else {
            popupMenu.inflate(R.menu.dnd_exceptions)
            popupMenu.setOnMenuItemClickListener {
                if (it.itemId != R.id.none) {
                    if (!containsCategory(PRIORITY_CATEGORY_CALLS) && category == PRIORITY_CATEGORY_CALLS) {
                        viewModel.profile!!.priorityCategories.add(PRIORITY_CATEGORY_CALLS)
                    } else if (!containsCategory(PRIORITY_CATEGORY_MESSAGES) && category == PRIORITY_CATEGORY_MESSAGES) {
                        viewModel.profile!!.priorityCategories.add(PRIORITY_CATEGORY_MESSAGES)
                    }
                }
                when (it.itemId) {
                    R.id.starred_contacts -> {
                        val text: String = "From starred contacts only"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            binding.exceptionsMessagesDescription.text = text
                            viewModel.profile!!.priorityMessageSenders = PRIORITY_SENDERS_STARRED
                        }
                        else {
                            binding.exceptionsCallsDescription.text = text
                            viewModel.profile!!.priorityCallSenders = PRIORITY_SENDERS_STARRED
                        }
                        true
                    }
                    R.id.contacts -> {
                        val text: String = "From contacts only"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            binding.exceptionsMessagesDescription.text = text
                            viewModel.profile!!.priorityMessageSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        else {
                            binding.exceptionsCallsDescription.text = text
                            viewModel.profile!!.priorityCallSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        true
                    }
                    R.id.anyone -> {
                        val text: String = "From anyone"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            binding.exceptionsMessagesDescription.text = text
                            viewModel.profile!!.priorityMessageSenders = PRIORITY_SENDERS_ANY
                        }
                        else {
                            binding.exceptionsCallsDescription.text = text
                            viewModel.profile!!.priorityCallSenders = PRIORITY_SENDERS_ANY
                        }
                        true
                    }
                    R.id.none -> {
                        val text: String = "None"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            binding.exceptionsMessagesDescription.text = text
                        }
                        else {
                            binding.exceptionsCallsDescription.text = text
                        }
                        viewModel.profile!!.priorityCategories.remove(category)
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            popupMenu.show()
        }
    }

    override fun onPrioritySelected(categories: ArrayList<Int>) {
        viewModel.profile!!.priorityCategories = categories
        binding.otherInterruptionsDescription.text = getPriorityCategoriesDescription(categories)
    }

    override fun onEffectsSelected(effects: ArrayList<Int>, type: Int) {
        if (type == 1) {
            viewModel.profile!!.screenOnVisualEffects = effects
            binding.whenScreenIsOnDescription?.text = getVisualEffectsDescription(effects, type)
        }
        else if (type == 0) {
            viewModel.profile!!.screenOffVisualEffects = effects
            binding.whenScreenIsOffDescription?.text = getVisualEffectsDescription(effects, type)
        }
    }

    companion object {

        private const val WHEN_SCREEN_IS_ON_FRAGMENT: Int = 1
        private const val WHEN_SCREEN_IS_OFF_FRAGMENT: Int = 2
        private const val OTHER_INTERRUPTIONS_FRAGMENT: Int = 0
    }
}