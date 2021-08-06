package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager.Policy.*
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
import com.example.volumeprofiler.fragments.dialogs.multiChoice.PriorityInterruptionsSelectionDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictionsDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictionsDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.interfaces.PriorityInterruptionsCallback
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.example.volumeprofiler.util.AnimUtils.Companion.scaleAnimation

@SuppressLint("UseSwitchCompatOrMaterialCode")
class ZenModePreferencesFragment: Fragment(), PriorityInterruptionsCallback, VisualRestrictionsCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var callbacks: EditProfileActivityCallbacks? = null
    private var whenScreenIsOffDescription: TextView? = null
    private var whenScreenIsOnDescription: TextView? = null
    private var exceptionsConversationsText: TextView? = null
    private var otherInterruptionsDescription: TextView? = null

    private lateinit var repeatCallersSwitch: Switch
    private lateinit var exceptionsCallsText: TextView
    private lateinit var exceptionsMessagesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun disableNestedScrolling(): Unit {
        val appBar: AppBarLayout = requireActivity().findViewById(R.id.app_bar)
        appBar.setExpanded(false, false)
        ViewCompat.setNestedScrollingEnabled(requireView().findViewById(R.id.nestedScrollView), false)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        callbacks = requireActivity() as EditProfileActivityCallbacks
        return if (Build.VERSION_CODES.M == Build.VERSION.SDK_INT) {
            inflater.inflate(R.layout.dnd_preferences_api_24, container, false)
        }
        else if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            inflater.inflate(R.layout.dnd_preferences_api_24, container, false)
        }
        else if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            inflater.inflate(R.layout.dnd_preferences_api_28, container, false)
        }
        else {
            inflater.inflate(R.layout.dnd_preferences_api_30, container, false)
        }
    }

    private fun initViews(view: View): Unit {
        exceptionsCallsText = view.findViewById(R.id.exceptionsCallsDescription)
        exceptionsMessagesText = view.findViewById(R.id.exceptionsMessagesDescription)
        exceptionsConversationsText = view.findViewById(R.id.exceptionsConversationsDescirption)
        whenScreenIsOffDescription = view.findViewById(R.id.whenScreenIsOffDescription)
        whenScreenIsOnDescription = view.findViewById(R.id.whenScreenIsOnDescription)
        otherInterruptionsDescription = view.findViewById(R.id.otherInterruptionsDescription)

        val screenOnLayout: RelativeLayout? = view.findViewById(R.id.blockWhenScreenIsOnLayout)
        val screenOnSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOnSwitch)
        val screenOffLayout: RelativeLayout? = view.findViewById(R.id.blockWhenScreenIsOffLayout)
        val screenOffSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOffSwitch)

        val callsLayout: LinearLayout = view.findViewById(R.id.exceptionsCallsLayout)
        val repeatCallers: RelativeLayout = view.findViewById(R.id.repeatCallersLayout)
        val messagesLayout: LinearLayout = view.findViewById(R.id.exceptionsMessagesLayout)
        val conversationsLayout: LinearLayout? = view.findViewById(R.id.exceptionsConversationsLayout)
        val otherInterruptionsLayout: LinearLayout = view.findViewById(R.id.otherInterruptionsLayout)

        repeatCallersSwitch = view.findViewById(R.id.repeatCallersSwitch)

        val whenScreenIsOnLayout: LinearLayout? = view.findViewById(R.id.whenScreenIsOnLayout)
        val whenScreenIsOffLayout: LinearLayout? = view.findViewById(R.id.whenScreenIsOffLayout)

        val onClickListener = View.OnClickListener {
            when (it.id) {
                screenOnLayout?.id -> {
                    if (screenOnSwitch != null) {
                        if (screenOnSwitch.isChecked) {
                            screenOnSwitch.isChecked = false
                            viewModel.mutableProfile!!.screenOnVisualEffects.remove(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                        else {
                            screenOnSwitch.isChecked = true
                            viewModel.mutableProfile!!.screenOffVisualEffects.add(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                    }
                }
                screenOffLayout?.id -> {
                    if (screenOffSwitch != null) {
                        if (screenOffSwitch.isChecked) {
                            screenOffSwitch.isChecked = false
                            viewModel.mutableProfile!!.screenOffVisualEffects.remove(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                        else {
                            screenOffSwitch.isChecked = true
                            viewModel.mutableProfile!!.screenOffVisualEffects.add(SUPPRESSED_EFFECT_SCREEN_ON)
                        }
                    }
                }
                callsLayout.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_CALLS, it)
                }
                messagesLayout.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_MESSAGES, it)
                }
                conversationsLayout?.id -> {
                    showPopupMenu(PRIORITY_CATEGORY_MESSAGES, it)
                }
                repeatCallers.id -> {
                    if (repeatCallersSwitch.isChecked) {
                        repeatCallersSwitch.isChecked = false
                        viewModel.mutableProfile!!.priorityCategories.remove(PRIORITY_CATEGORY_REPEAT_CALLERS)
                    }
                    else {
                        repeatCallersSwitch.isChecked = true
                        viewModel.mutableProfile!!.priorityCategories.add(PRIORITY_CATEGORY_REPEAT_CALLERS)
                    }
                }
                otherInterruptionsLayout.id -> {
                    val fragment = PriorityInterruptionsSelectionDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, OTHER_INTERRUPTIONS_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                whenScreenIsOffLayout?.id -> {
                    val fragment = ScreenOffVisualRestrictionsDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, WHEN_SCREEN_IS_OFF_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                whenScreenIsOnLayout?.id -> {
                    val fragment = ScreenOnVisualRestrictionsDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@ZenModePreferencesFragment, WHEN_SCREEN_IS_ON_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
        }
        screenOffLayout?.setOnClickListener(onClickListener)
        screenOnLayout?.setOnClickListener(onClickListener)
        callsLayout.setOnClickListener(onClickListener)
        repeatCallers.setOnClickListener(onClickListener)
        messagesLayout.setOnClickListener(onClickListener)
        conversationsLayout?.setOnClickListener(onClickListener)
        otherInterruptionsLayout.setOnClickListener(onClickListener)
        whenScreenIsOffLayout?.setOnClickListener(onClickListener)
        whenScreenIsOnLayout?.setOnClickListener(onClickListener)
    }

    private fun updateUI(): Unit {
        otherInterruptionsDescription?.text = getPriorityCategoriesDescription(viewModel.mutableProfile!!.priorityCategories)
        exceptionsCallsText.text = if (!containsCategory(PRIORITY_CATEGORY_CALLS)) "None" else when (viewModel.mutableProfile!!.priorityCallSenders) {
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
        exceptionsMessagesText.text = if (!containsCategory(PRIORITY_CATEGORY_MESSAGES)) "None" else when (viewModel.mutableProfile!!.priorityMessageSenders) {
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
        exceptionsConversationsText?.text = when (viewModel.mutableProfile!!.primaryConversationSenders) {
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
        whenScreenIsOnDescription?.text = getVisualEffectsDescription(viewModel.mutableProfile!!.screenOnVisualEffects, 1)
        whenScreenIsOffDescription?.text = getVisualEffectsDescription(viewModel.mutableProfile!!.screenOffVisualEffects, 0)
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
        val list: List<Int> = viewModel.mutableProfile!!.priorityCategories
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
                        exceptionsConversationsText?.text = "Anyone"
                        viewModel.mutableProfile!!.primaryConversationSenders = CONVERSATION_SENDERS_ANYONE
                        true
                    }
                    R.id.priority_conversations -> {
                        exceptionsConversationsText?.text = "Important"
                        viewModel.mutableProfile!!.primaryConversationSenders = CONVERSATION_SENDERS_IMPORTANT
                        true
                    }
                    R.id.none -> {
                        exceptionsConversationsText?.text = "None"
                        viewModel.mutableProfile!!.primaryConversationSenders = CONVERSATION_SENDERS_NONE
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
                        viewModel.mutableProfile!!.priorityCategories.add(PRIORITY_CATEGORY_CALLS)
                    } else if (!containsCategory(PRIORITY_CATEGORY_MESSAGES) && category == PRIORITY_CATEGORY_MESSAGES) {
                        viewModel.mutableProfile!!.priorityCategories.add(PRIORITY_CATEGORY_MESSAGES)
                    }
                }
                when (it.itemId) {
                    R.id.starred_contacts -> {
                        val text: String = "From starred contacts only"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            exceptionsMessagesText.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_STARRED
                        }
                        else {
                            exceptionsCallsText.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_STARRED
                        }
                        true
                    }
                    R.id.contacts -> {
                        val text: String = "From contacts only"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            exceptionsMessagesText.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        else {
                            exceptionsCallsText.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        true
                    }
                    R.id.anyone -> {
                        val text: String = "From anyone"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            exceptionsMessagesText.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_ANY
                        }
                        else {
                            exceptionsCallsText.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_ANY
                        }
                        true
                    }
                    R.id.none -> {
                        val text: String = "None"
                        if (category == PRIORITY_CATEGORY_MESSAGES) {
                            exceptionsMessagesText.text = text
                        }
                        else {
                            exceptionsCallsText.text = text
                        }
                        viewModel.mutableProfile!!.priorityCategories.remove(category)
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
        viewModel.mutableProfile!!.priorityCategories = categories
        otherInterruptionsDescription?.text = getPriorityCategoriesDescription(categories)
    }

    override fun onEffectsSelected(effects: ArrayList<Int>, type: Int) {
        if (type == 1) {
            viewModel.mutableProfile!!.screenOnVisualEffects = effects
            whenScreenIsOnDescription?.text = getVisualEffectsDescription(effects, type)
        }
        else if (type == 0) {
            viewModel.mutableProfile!!.screenOffVisualEffects = effects
            whenScreenIsOffDescription?.text = getVisualEffectsDescription(effects, type)
        }
    }

    override fun onDestroyView() {
        callbacks = null
        super.onDestroyView()
    }

    companion object {

        private const val WHEN_SCREEN_IS_ON_FRAGMENT: Int = 1
        private const val WHEN_SCREEN_IS_OFF_FRAGMENT: Int = 2
        private const val OTHER_INTERRUPTIONS_FRAGMENT: Int = 0
    }
}