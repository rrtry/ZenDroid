package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import android.app.NotificationManager.Policy.*
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.fragments.dialogs.multiChoice.OtherInterruptionsSelectionDialog
import com.example.volumeprofiler.interfaces.FragmentTransition
import com.example.volumeprofiler.interfaces.OtherInterruptionsCallback
import com.example.volumeprofiler.util.ProfileUtil.Companion.updatePolicy

@SuppressLint("UseSwitchCompatOrMaterialCode")
class DnDPreferencesFragment: Fragment(), OtherInterruptionsCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var exceptionsCallsText: TextView? = null
    private var exceptionsMessagesText: TextView? = null
    private var exceptionsConversationsText: TextView? = null
    private var callbacks: FragmentTransition? = null

    override fun onDestroyView() {
        callbacks = null
        exceptionsCallsText = null
        exceptionsMessagesText = null
        exceptionsConversationsText = null
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        callbacks = requireActivity() as FragmentTransition
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
        initViews(view)
        return view
    }

    private fun initViews(view: View): Unit {
        exceptionsCallsText = view.findViewById(R.id.exceptionsCallsDescription)
        exceptionsMessagesText = view.findViewById(R.id.exceptionsMessagesDescription)
        exceptionsConversationsText = view.findViewById(R.id.exceptionsConversationsDescirption)

        val screenOnLayout: RelativeLayout? = view.findViewById(R.id.blockWhenScreenIsOnLayout)
        val screenOnSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOnSwitch)
        val screenOffLayout: RelativeLayout? = view.findViewById(R.id.blockWhenScreenIsOffLayout)
        val screenOffSwitch: Switch? = view.findViewById(R.id.blockWhenScreenIsOffSwitch)

        val callsLayout: LinearLayout = view.findViewById(R.id.exceptionsCallsLayout)
        val repeatCallers: RelativeLayout = view.findViewById(R.id.repeatCallersLayout)
        val messagesLayout: LinearLayout = view.findViewById(R.id.exceptionsMessagesLayout)
        val conversationsLayout: LinearLayout? = view.findViewById(R.id.exceptionsConversationsLayout)
        val otherInterruptionsLayout: LinearLayout = view.findViewById(R.id.otherInterruptionsLayout)

        val repeatCallersSwitch: Switch = view.findViewById(R.id.repeatCallersSwitch)

        val noSoundsFromNotificationsLayout: RelativeLayout? = view.findViewById(R.id.noSoundsFromNotificationLayout)
        val noVisualsAndSoundsLayout: RelativeLayout? = view.findViewById(R.id.noVisualsAndSoundsFromNotificationsLayout)
        val customPolicyLayout: ConstraintLayout? = view.findViewById(R.id.customPolicyLayout)

        val noSoundsButton: RadioButton? = view.findViewById(R.id.noSoundsButton)
        val noVisualsAndSoundsButton: RadioButton? = view.findViewById(R.id.noVisualsAndSoundsButton)
        val customPolicyButton: RadioButton? = view.findViewById(R.id.customPolicyButton)

        val customPolicySettingsButton: ImageButton? = view.findViewById(R.id.customPolicySettingsButton)
        val onClickListener = View.OnClickListener {
            when (it.id) {
                screenOnLayout?.id -> {
                    if (screenOnSwitch != null) {
                        val str: String = viewModel.mutableProfile!!.suppressedVisualEffects!!
                        val policy: String = if (screenOnSwitch.isChecked) {
                            screenOnSwitch.isChecked = false
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_ON, false)
                        } else {
                            screenOnSwitch.isChecked = true
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_ON, true)
                        }
                        viewModel.mutableProfile!!.suppressedVisualEffects = policy
                    }
                }
                screenOffLayout?.id -> {
                    if (screenOffSwitch != null) {
                        val str: String = viewModel.mutableProfile!!.suppressedVisualEffects!!
                        val policy: String = if (screenOffSwitch.isChecked) {
                            screenOffSwitch.isChecked = false
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_OFF, false)
                        } else {
                            screenOffSwitch.isChecked = true
                            updatePolicy(str, SUPPRESSED_EFFECT_SCREEN_OFF, true)
                        }
                        viewModel.mutableProfile!!.suppressedVisualEffects = policy
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
                noSoundsFromNotificationsLayout?.id -> {

                }
                noVisualsAndSoundsLayout?.id -> {
                    if (noVisualsAndSoundsButton != null) {
                        val list: List<Int> = listOf(
                                SUPPRESSED_EFFECT_SCREEN_OFF,
                                SUPPRESSED_EFFECT_SCREEN_ON,
                                SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
                                SUPPRESSED_EFFECT_LIGHTS,
                                SUPPRESSED_EFFECT_PEEK,
                                SUPPRESSED_EFFECT_STATUS_BAR,
                                SUPPRESSED_EFFECT_BADGE,
                                SUPPRESSED_EFFECT_AMBIENT,
                                SUPPRESSED_EFFECT_NOTIFICATION_LIST
                        )
                        val policy: String = if (noVisualsAndSoundsButton.isChecked) {
                            noVisualsAndSoundsButton.isChecked = false
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, list, false)
                        }
                        else {
                            noVisualsAndSoundsButton.isChecked = true
                            updatePolicy(viewModel.mutableProfile!!.suppressedVisualEffects!!, list, true)
                        }
                        viewModel.mutableProfile!!.suppressedVisualEffects = policy
                    }
                }
                customPolicyLayout?.id -> {

                }
                customPolicySettingsButton?.id -> {
                    callbacks?.onFragmentReplace(EditProfileActivity.CUSTOM_RESTRICTIONS_FRAGMENT)
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
        noSoundsFromNotificationsLayout?.setOnClickListener(onClickListener)
        noVisualsAndSoundsLayout?.setOnClickListener(onClickListener)
        customPolicyLayout?.setOnClickListener(onClickListener)
        customPolicySettingsButton?.setOnClickListener(onClickListener)
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
                            exceptionsMessagesText!!.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_STARRED
                        }
                        else {
                            exceptionsCallsText!!.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_STARRED
                        }
                        true
                    }
                    R.id.contacts -> {
                        val text: String = "Contacts"
                        if (category == CATEGORY_MESSAGES) {
                            exceptionsMessagesText!!.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        else {
                            exceptionsCallsText!!.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_CONTACTS
                        }
                        true
                    }
                    R.id.anyone -> {
                        val text: String = "Anyone"
                        if (category == CATEGORY_MESSAGES) {
                            exceptionsMessagesText!!.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = PRIORITY_SENDERS_ANY
                        }
                        else {
                            exceptionsCallsText!!.text = text
                            viewModel.mutableProfile!!.priorityCallSenders = PRIORITY_SENDERS_ANY
                        }
                        true
                    }
                    R.id.none -> {
                        val text: String = "None"
                        if (category == CATEGORY_MESSAGES) {
                            exceptionsMessagesText!!.text = text
                            viewModel.mutableProfile!!.priorityMessageSenders = -1
                        }
                        else {
                            exceptionsCallsText!!.text = text
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
        Log.i("DnDPreferencesFragment", categories)
    }

    companion object {

        private const val OTHER_INTERRUPTIONS_FRAGMENT: Int = 0
        private const val CATEGORY_CALLS: Int = 0
        private const val CATEGORY_MESSAGES: Int = 1
        private const val CATEGORY_CONVERSATIONS: Int = 2
    }
}