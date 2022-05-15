package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.WeekDay.*
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WeekDaysPickerDialog: BaseDialog() {

    private val viewModel: AlarmDetailsViewModel by activityViewModels()

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
        viewModel.scheduledDays.value = mask
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