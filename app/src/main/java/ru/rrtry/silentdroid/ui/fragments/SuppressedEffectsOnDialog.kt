package ru.rrtry.silentdroid.ui.fragments

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOnDialog : BaseDialog() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    override val title: String = "When screen is on"
    override val arrayRes: Int = R.array.screenIsOn
    override val values: List<Int> = listOf(
        SUPPRESSED_EFFECT_BADGE,
        SUPPRESSED_EFFECT_STATUS_BAR,
        SUPPRESSED_EFFECT_PEEK,
        SUPPRESSED_EFFECT_NOTIFICATION_LIST
    )

    override fun applyChanges(mask: Int) {
        viewModel.suppressedVisualEffects.value = mask
    }

    override fun onValueAdded(position: Int, value: Int) {

    }

    override fun onValueRemoved(position: Int, value: Int) {

    }

    /*
    override fun onResume() {
        val listView: ListView = getListView()
        val arrayAdapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            requireContext(), R.layout.dialog_multichoice_item
        ) {

            override fun isEnabled(position: Int): Boolean {
                val enabled: Boolean = isListItemEnabled(position)
                if (!enabled) {
                    listView.setItemChecked(position, true)
                }
                return enabled
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view: View = super.getView(position, convertView, parent)
                view.isEnabled = isListItemEnabled(position)
                return view
            }
        }
        resources.getStringArray(arrayRes).forEach {
            arrayAdapter.add(it)
        }
        listView.adapter = arrayAdapter
        super.onResume()
    }

    private fun isListItemEnabled(position: Int): Boolean {
        return !(values[position] == SUPPRESSED_EFFECT_STATUS_BAR
                && ((mask and SUPPRESSED_EFFECT_NOTIFICATION_LIST) != 0))
    }
     */

    companion object {

        fun newInstance(profile: Profile): SuppressedEffectsOnDialog {
            return SuppressedEffectsOnDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_MASK, profile.suppressedVisualEffects)
                }
            }
        }
    }
}