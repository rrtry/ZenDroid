package com.example.volumeprofiler.fragments

import android.os.Bundle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.AlarmDetailsActivity
import com.example.volumeprofiler.util.WeekDay.*

class WeekDaysPickerDialog: BaseDialog() {

    override val title: String = "Weekdays"
    override val arrayRes: Int = R.array.daysOfWeek
    override val categories: List<Int> = listOf(
        MONDAY.value,
        TUESDAY.value,
        WEDNESDAY.value,
        THURSDAY.value,
        FRIDAY.value,
        SATURDAY.value,
        SUNDAY.value
    )

    override fun applyChanges(mask: Int) {
        parentFragmentManager.setFragmentResult(AlarmDetailsActivity.SCHEDULED_DAYS_REQUEST_KEY, Bundle().apply {
            putInt(EXTRA_MASK, categoriesMask)
        })
    }

    companion object {

        fun newInstance(days: Int): WeekDaysPickerDialog {
            return WeekDaysPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_MASK, days)
                }
            }
        }
    }
}