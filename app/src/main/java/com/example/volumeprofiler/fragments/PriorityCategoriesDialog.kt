package com.example.volumeprofiler.fragments

import android.os.Build
import android.os.Bundle
import com.example.volumeprofiler.R
import android.app.NotificationManager.Policy.*
import com.example.volumeprofiler.entities.Profile

class PriorityCategoriesDialog: BaseDialog() {

    override val title: String = "Priority categories"
    override val arrayRes: Int = if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
        R.array.priorityCategoriesApi28
    } else {
        R.array.priorityCategoriesApi23
    }

    override val categories: List<Int> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
        parentFragmentManager.setFragmentResult(
            InterruptionFilterFragment.PRIORITY_REQUEST_KEY,
            Bundle().apply {
                putInt(EXTRA_MASK, mask)
            })
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