package ru.rrtry.silentdroid.ui.fragments

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.WeekDay.*
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WeekDaysPickerDialog: BaseDialog() {

    private val viewModel: AlarmDetailsViewModel by activityViewModels()

    override val title: String = "Weekdays"
    override val arrayRes: Int = R.array.daysOfWeek
    override val values: List<Int> = listOf(
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

    override fun onValueAdded(position: Int, value: Int) {

    }

    override fun onValueRemoved(position: Int, value: Int) {

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