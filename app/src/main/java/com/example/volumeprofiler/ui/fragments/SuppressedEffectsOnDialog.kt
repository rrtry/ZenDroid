package com.example.volumeprofiler.ui.fragments

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOnDialog : BaseDialog() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    override val title: String = "When screen is on"
    override val arrayRes: Int = R.array.screenIsOn
    override val categories: List<Int> = listOf(
        SUPPRESSED_EFFECT_BADGE,
        SUPPRESSED_EFFECT_STATUS_BAR,
        SUPPRESSED_EFFECT_PEEK,
        SUPPRESSED_EFFECT_NOTIFICATION_LIST
    )

    private fun isListItemEnabled(position: Int): Boolean {
        return !(categories[position] == SUPPRESSED_EFFECT_STATUS_BAR
                && ((categoriesMask and SUPPRESSED_EFFECT_NOTIFICATION_LIST) != 0))
    }

    override fun onResume() {
        val listView: ListView = getListView()
        val arrayAdapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_list_item_multiple_choice
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

    override fun applyChanges(mask: Int) {
        viewModel.suppressedVisualEffects.value = mask
    }

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