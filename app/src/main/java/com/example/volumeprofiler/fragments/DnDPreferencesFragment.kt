package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.CustomPolicyDialogCallbacks
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import android.app.NotificationManager.Policy.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kotlin.collections.ArrayList

@SuppressLint("UseSwitchCompatOrMaterialCode")
class DnDPreferencesFragment: Fragment(), CustomPolicyDialogCallbacks {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private lateinit var exceptionsCallsText: TextView
    private lateinit var exceptionsMessagesText: TextView
    private var exceptionsConversationsText: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (viewModel.mutableProfile == null) {
            Log.i("DnDPreferencesFragment", "mutable profile is null")
        }
        val view: View = if (Build.VERSION_CODES.M == Build.VERSION.SDK_INT) {
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
        initializeUI(view)
        return view
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
                        val text: String = "Starred contacts"
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
                        val text: String = "Contacts"
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
                        val text: String = "Anyone"
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
                        val text: String = "None"
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

    private fun initializeUI(view: View): Unit {
        exceptionsCallsText = view.findViewById(R.id.exceptionsCallsDescription)
        exceptionsMessagesText = view.findViewById(R.id.exceptionsMessagesDescription)
        exceptionsConversationsText = view.findViewById(R.id.exceptionsConversationsDescirption)

        val callsLayout: LinearLayout = view.findViewById(R.id.exceptionsCallsLayout)
        val messagesLayout: LinearLayout = view.findViewById(R.id.exceptionsMessagesLayout)
        val conversationLayout: LinearLayout? = view.findViewById(R.id.exceptionsConversationsLayout)

        val repeatCallersSwitch: Switch = view.findViewById(R.id.repeatCallersSwitch)
        val remindersSwitch: Switch = view.findViewById(R.id.RemindersSwitch)
        val calendarEventsSwitch: Switch = view.findViewById(R.id.EventsSwitch)
        val screenOnSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOffSwitch)
        val screenOffSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOffSwitch)
        val alarmsSwitch: Switch? = view.findViewById(R.id.alarmsSwitch)
        val mediaSoundsSwitch: Switch? = view.findViewById(R.id.mediaSoundsSwitch)
        val touchSoundsSwitch: Switch? = view.findViewById(R.id.touchSoundsSwitch)

        val policyNoSoundsButton: RadioButton? = view.findViewById(R.id.noSoundsButton)
        val policyNoVisualsButton: RadioButton? = view.findViewById(R.id.noVisualsAndSoundsButton)
        val policyCustomButton: RadioButton? = view.findViewById(R.id.customPolicyButton)

        val onClickListener: View.OnClickListener = View.OnClickListener {

            when (it.id) {
                callsLayout.id -> {
                    showPopupMenu(CATEGORY_CALLS, it)
                }
                conversationLayout?.id -> {
                    showPopupMenu(CATEGORY_CONVERSATIONS, it)
                }
                messagesLayout.id -> {
                    showPopupMenu(CATEGORY_MESSAGES, it)
                }
            }
        }
        val onCheckedChangeListener = CompoundButton.OnCheckedChangeListener {
            buttonView, isChecked ->
            if (buttonView.isPressed) {

                when (buttonView.id) {

                    R.id.noSoundsButton -> {
                        val policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }

                    R.id.noVisualsAndSoundsButton -> {
                        val list: List<Int> = listOf(
                                SUPPRESSED_EFFECT_SCREEN_OFF,
                                SUPPRESSED_EFFECT_SCREEN_ON,
                                SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
                                SUPPRESSED_EFFECT_LIGHTS,
                                SUPPRESSED_EFFECT_PEEK,
                                SUPPRESSED_EFFECT_STATUS_BAR,
                                SUPPRESSED_EFFECT_BADGE,
                                SUPPRESSED_EFFECT_AMBIENT,
                                SUPPRESSED_EFFECT_NOTIFICATION_LIST)
                        if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, list, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, list, false)
                        }
                    }

                    R.id.customPolicyButton -> {
                        val dialog: DialogFragment = CustomRestrictionsDialog.newInstance().apply {
                            this.setTargetFragment(this@DnDPreferencesFragment, 2)
                        }
                        dialog.show(requireActivity().supportFragmentManager, null)
                    }

                    R.id.repeatCallersSwitch -> {
                        val policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }

                    R.id.RemindersSwitch -> {
                        var policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REMINDERS, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_REMINDERS, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }

                    R.id.EventsSwitch -> {
                        var policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_EVENTS, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_EVENTS, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }

                    R.id.blockWhenScreenIsOnSwitch -> {
                        val policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, SUPPRESSED_EFFECT_SCREEN_ON, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, SUPPRESSED_EFFECT_SCREEN_ON, false)
                        }
                        viewModel.mutableProfile!!.suppressedVisualEffects = policy
                    }
                    R.id.blockWhenScreenIsOffSwitch-> {
                        var policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, SUPPRESSED_EFFECT_SCREEN_OFF, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, SUPPRESSED_EFFECT_SCREEN_OFF, false)
                        }
                        viewModel.mutableProfile!!.suppressedVisualEffects = policy
                    }
                    R.id.alarmsSwitch-> {
                        val policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_ALARMS, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_ALARMS, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }
                    R.id.mediaSoundsSwitch -> {
                        var policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_MEDIA, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_MEDIA, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }
                    R.id.touchSoundsSwitch -> {
                        var policy: String = if (isChecked) {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_SYSTEM, true)
                        } else {
                            updatePolicy(viewModel.mutableProfile!!.priorityCategories, PRIORITY_CATEGORY_SYSTEM, false)
                        }
                        viewModel.mutableProfile!!.priorityCategories = policy
                    }
                }
            }
        }
        callsLayout.setOnClickListener(onClickListener)
        messagesLayout.setOnClickListener(onClickListener)
        conversationLayout?.setOnClickListener(onClickListener)

        repeatCallersSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        remindersSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        calendarEventsSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        screenOnSwitch?.setOnCheckedChangeListener(onCheckedChangeListener)
        screenOffSwitch?.setOnCheckedChangeListener(onCheckedChangeListener)
        alarmsSwitch?.setOnCheckedChangeListener(onCheckedChangeListener)
        mediaSoundsSwitch?.setOnCheckedChangeListener(onCheckedChangeListener)
        touchSoundsSwitch?.setOnCheckedChangeListener(onCheckedChangeListener)

        policyNoSoundsButton?.setOnCheckedChangeListener(onCheckedChangeListener)
        policyNoVisualsButton?.setOnCheckedChangeListener(onCheckedChangeListener)
        policyCustomButton?.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private fun updatePolicy(initialString: String, value: Int, append: Boolean): String {
        val strInt: String = value.toString()
        var result: String = ""
        if (append) {
            val stringBuilder: StringBuilder = StringBuilder(initialString)
            stringBuilder.append("$value,")
            result = stringBuilder.toString()
        }
        else {
            if (initialString.isNotEmpty()) {
                val list: ArrayList<String> = initialString.split(",") as ArrayList<String>
                for (i in list) {
                    if (i == strInt) {
                        list.remove(i)
                        break
                    }
                }
                result = list.joinToString(",")
            }
        }
        return result
    }

    private fun updatePolicy(initialString: String, values: List<Int>, append: Boolean): String {
        var result: String = ""
        if (append) {
            val stringBuilder: StringBuilder = java.lang.StringBuilder(initialString)
            for (i in values) {
                stringBuilder.append(i)
            }
            result = stringBuilder.toString()
        }
        else {
            if (initialString.isNotEmpty()) {
                val list: ArrayList<String> = initialString.split(",") as ArrayList<String>
                for (i in list) {
                    for (j in values) {
                        if (i == j.toString()) {
                            list.remove(i)
                        }
                    }
                }
                result = list.joinToString(",")
            }
        }
        return result
    }

    override fun onPolicySelected(selectedItems: String) {
        Log.i("DnDPreferencesFragment", selectedItems)
    }

    companion object {

        private const val CATEGORY_CALLS: Int = 0
        private const val CATEGORY_MESSAGES: Int = 1
        private const val CATEGORY_CONVERSATIONS: Int = 2
    }
}