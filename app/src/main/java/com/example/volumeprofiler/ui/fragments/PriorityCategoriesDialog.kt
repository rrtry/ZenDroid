package com.example.volumeprofiler.ui.fragments

import android.os.Build
import android.os.Bundle
import com.example.volumeprofiler.R
import android.app.NotificationManager.Policy.*
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PriorityCategoriesDialog: BaseDialog() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    override val title: String = "Priority categories"
    override val arrayRes: Int = if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
        R.array.priorityCategoriesApi28
    } else {
        R.array.priorityCategoriesApi23
    }

    override val values: List<Int> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        listOf(
            PRIORITY_CATEGORY_ALARMS,
            PRIORITY_CATEGORY_MEDIA,
            PRIORITY_CATEGORY_SYSTEM,
            PRIORITY_CATEGORY_REMINDERS,
            PRIORITY_CATEGORY_EVENTS
        )
    } else {
        listOf(
            PRIORITY_CATEGORY_REMINDERS,
            PRIORITY_CATEGORY_EVENTS
        )
    }

    override fun applyChanges(mask: Int) {
        viewModel.priorityCategories.value = mask
    }

    override fun onValueAdded(position: Int, value: Int) {

    }

    override fun onValueRemoved(position: Int, value: Int) {

    }

    companion object {

        fun newInstance(profile: Profile): PriorityCategoriesDialog {
            return PriorityCategoriesDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_MASK, profile.priorityCategories)
                }
            }
        }
    }
}