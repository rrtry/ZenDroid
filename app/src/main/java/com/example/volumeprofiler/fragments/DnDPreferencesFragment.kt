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
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.dialogs.multiChoice.OtherInterruptionsSelectionDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictionsDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictionsDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.interfaces.OtherInterruptionsCallback
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.util.ProfileUtil.Companion.updatePolicy
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.example.volumeprofiler.util.AnimationUtils.Companion.scaleAnimation

@SuppressLint("UseSwitchCompatOrMaterialCode")
class DnDPreferencesFragment: Fragment(), OtherInterruptionsCallback, VisualRestrictionsCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var whenScreenIsOffDescription: TextView? = null
    private var whenScreenIsOnDescription: TextView? = null
    private lateinit var repeatCallersSwitch: Switch
    private var otherInterruptionsDescription: TextView? = null
    private lateinit var exceptionsCallsText: TextView
    private lateinit var exceptionsMessagesText: TextView
    private var exceptionsConversationsText: TextView? = null
    private var callbacks: EditProfileActivityCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun disableNestedScrolling(): Unit {
        val appBar: AppBarLayout = requireActivity().findViewById(R.id.app_bar)
        appBar.setExpanded(false, false)
        if (Build.VERSION_CODES.M < Build.VERSION.SDK_INT) {
            ViewCompat.setNestedScrollingEnabled(requireView().findViewById(R.id.nestedScrollView), false)
        }
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
        disableNestedScrolling()
        initViews(view)
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
                        val str: String = viewModel.mutableProfile!!.screenOnVisualEffects
                        val policy: String = if (screenOnSwitch.isChecked) {
                            screenOnSwitch.isChecked = false
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_ON, false)
                        } else {
                            screenOnSwitch.isChecked = true
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_ON, true)
                        }
                        viewModel.mutableProfile!!.screenOnVisualEffects = policy
                    }
                }
                screenOffLayout?.id -> {
                    if (screenOffSwitch != null) {
                        val str: String = viewModel.mutableProfile!!.screenOnVisualEffects
                        val policy: String = if (screenOffSwitch.isChecked) {
                            screenOffSwitch.isChecked = false
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_OFF, false)
                        } else {
                            screenOffSwitch.isChecked = true
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_OFF, true)
                        }
                        viewModel.mutableProfile!!.screenOnVisualEffects = policy
                    }
                }
                callsLayout.id -> {
                    showPopupMenu(CATEGORY_CALLS, it)
                }
                messagesLayout.id -> {
                    showPopupMenu(CATEGORY_MESSAGES, it)
                }
                conversationsLayout?.id -> {
                    showPopupMenu(CATEGORY_CONVERSATIONS, it)
                }
                repeatCallers.id -> {
                    val policy: String = if (repeatCallersSwitch.isChecked) {
                        repeatCallersSwitch.isChecked = false
                        updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, false)
                    } else {
                        repeatCallersSwitch.isChecked = true
                        updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, true)
                    }
                    viewModel.mutableProfile!!.priorityCategories = policy
                }
                otherInterruptionsLayout.id -> {
                    val fragment = OtherInterruptionsSelectionDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@DnDPreferencesFragment, OTHER_INTERRUPTIONS_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                whenScreenIsOffLayout?.id -> {
                    val fragment = ScreenOffVisualRestrictionsDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@DnDPreferencesFragment, WHEN_SCREEN_IS_OFF_FRAGMENT)
                    }
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
                whenScreenIsOnLayout?.id -> {
                    val fragment = ScreenOnVisualRestrictionsDialog.newInstance(viewModel.mutableProfile!!).apply {
                        this.setTargetFragment(this@DnDPreferencesFragment, WHEN_SCREEN_IS_ON_FRAGMENT)
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
        val callSenders: Int = viewModel.mutableProfile!!.priorityCallSenders
        val messageSenders: Int = viewModel.mutableProfile!!.priorityMessageSenders
        val conversationSenders: Int = viewModel.mutableProfile!!.primaryConversationSenders
        val priorityCategories: String = viewModel.mutableProfile!!.priorityCategories
        if (priorityCategories.isNotEmpty()) {
            val priorityCategoriesList: List<Int> = toList(priorityCategories)
            otherInterruptionsDescription?.text = updatePriorityCategoriesDescription(priorityCategoriesList)
        }
        else {
            otherInterruptionsDescription?.text = "Restrict everything"
        }
        exceptionsCallsText.text = when (callSenders) {
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
        exceptionsMessagesText.text = when (messageSenders) {
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
        exceptionsConversationsText?.text = when (conversationSenders) {
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
        if (viewModel.mutableProfile!!.screenOnVisualEffects.isNotEmpty()) {
            val screenOnVisualEffects: String = viewModel.mutableProfile!!.screenOnVisualEffects
            val list: List<Int> = toList(screenOnVisualEffects)
            whenScreenIsOnDescription?.text = updateVisualEffectsDescription(list, 1)
        }
        else {
            whenScreenIsOnDescription?.text = "None"
        }
        if (viewModel.mutableProfile!!.screenOffVisualEffects.isNotEmpty()) {
            val screenOffVisualEffects: String = viewModel.mutableProfile!!.screenOffVisualEffects
            val list: List<Int> = toList(screenOffVisualEffects)
            whenScreenIsOffDescription?.text = updateVisualEffectsDescription(list, 0)
        }
        else {
            whenScreenIsOffDescription?.text = "None"
        }
    }

    private fun toList(string: String): List<Int> {
        return string.split(",").mapNotNull {
            try {
                it.toInt()
            }
            catch (e: NumberFormatException) {
                null
            }
        }
    }

    private fun updatePriorityCategoriesDescription(list: List<Int>): String {
        val stringBuilder: StringBuilder = StringBuilder()
        stringBuilder.append("Allow ")
        for (i in list) {
            when (i) {
                PRIORITY_CATEGORY_ALARMS -> {
                    stringBuilder.append("alarms, ")
                }
                PRIORITY_CATEGORY_MEDIA -> {
                    stringBuilder.append("media, ")
                }
                PRIORITY_CATEGORY_SYSTEM -> {
                    stringBuilder.append("touch sounds, ")
                }
                PRIORITY_CATEGORY_REMINDERS -> {
                    stringBuilder.append("reminders, ")
                }
                PRIORITY_CATEGORY_EVENTS -> {
                    stringBuilder.append("events, ")
                }
            }
        }
        if (stringBuilder.isEmpty()) {
            stringBuilder.append("None")
        }
        else {
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        }
        return stringBuilder.toString()
    }

    private fun updateVisualEffectsDescription(list: List<Int>, type: Int): String {
        val stringBuilder: StringBuilder = StringBuilder()
        if (type == 0) {
            for (i in list) {
                when (i) {
                    SUPPRESSED_EFFECT_LIGHTS -> {
                        stringBuilder.append("Don\'t blink notification light, ")
                    }
                    SUPPRESSED_EFFECT_FULL_SCREEN_INTENT -> {
                        stringBuilder.append("Don\'t turn on screen, ")
                    }
                    SUPPRESSED_EFFECT_AMBIENT -> {
                        stringBuilder.append("Don\'t wake for notifications, ")
                    }
                }
            }
        }
        else {
            for (i in list) {
                when (i) {
                    SUPPRESSED_EFFECT_BADGE -> {
                        stringBuilder.append("Hide notification dots, ")
                    }
                    SUPPRESSED_EFFECT_STATUS_BAR -> {
                        stringBuilder.append("Hide status bar icons, ")
                    }
                    SUPPRESSED_EFFECT_PEEK -> {
                        stringBuilder.append("Don\'t pop notifications on screen, ")
                    }
                    SUPPRESSED_EFFECT_NOTIFICATION_LIST -> {
                        stringBuilder.append("Hide from notifications list, ")
                    }
                }
            }
        }
        if (stringBuilder.isEmpty()) {
            stringBuilder.append("None")
        }
        else {
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        }
        return stringBuilder.toString()
    }

    private fun showPopupMenu(category: Int, view: View): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        if (category == CATEGORY_CONVERSATIONS) {
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
                when (it.itemId) {

                    R.id.starred_contacts -> {
                        val text: String = "From starred contacts only"
                        if (category == CATEGORY_MESSAGES) {
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
                        if (category == CATEGORY_MESSAGES) {
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
                        if (category == CATEGORY_MESSAGES) {
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
                        val text: String = "Block everything"
                        if (category == CATEGORY_MESSAGES) {
                            exceptionsMessagesText.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = -1
                        }
                        else {
                            exceptionsCallsText.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = -1
                        }
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

    override fun onPrioritySelected(categories: String) {
        viewModel.mutableProfile!!.priorityCategories = categories
        updateUI()
    }

    override fun onEffectsSelected(effects: String, type: Int) {
        if (type == 1) {
            viewModel.mutableProfile!!.screenOnVisualEffects = effects
        }
        else if (type == 0) {
            viewModel.mutableProfile!!.screenOffVisualEffects = effects
        }
        updateUI()
    }

    override fun onDestroyView() {
        callbacks = null
        super.onDestroyView()
    }

    companion object {

        private const val WHEN_SCREEN_IS_ON_FRAGMENT: Int = 1
        private const val WHEN_SCREEN_IS_OFF_FRAGMENT: Int = 2
        private const val OTHER_INTERRUPTIONS_FRAGMENT: Int = 0
        private const val CATEGORY_CALLS: Int = 0
        private const val CATEGORY_MESSAGES: Int = 1
        private const val CATEGORY_CONVERSATIONS: Int = 2
    }
}