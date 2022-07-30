package ru.rrtry.silentdroid.ui.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType.START_TIME
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType.END_TIME
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel

@AndroidEntryPoint
class TimePickerFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private val viewModel: AlarmDetailsViewModel by activityViewModels()
    private lateinit var dialogType: DialogType

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val localTime: LocalTime = requireArguments().getSerializable(ARG_LOCAL_TIME) as? LocalTime
            ?: throw IllegalArgumentException("localtime not specified")

        dialogType = requireArguments().getSerializable(ARG_DIALOG_TYPE) as? DialogType
            ?: throw IllegalArgumentException("dialogType not specified")

        return TimePickerDialog(
            context,
            this, localTime.hour, localTime.minute,
            DateFormat.is24HourFormat(context))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        setResult(LocalTime.of(hourOfDay, minute))
    }

    private fun setResult(adjustedLocalTime: LocalTime) {
        if (dialogType == START_TIME) {
            viewModel.startTime.value = adjustedLocalTime
        }
        else if (dialogType == END_TIME) {
            viewModel.endTime.value = adjustedLocalTime
        }
    }

    companion object {

        private const val ARG_LOCAL_TIME: String = "arg_local_time"
        private const val ARG_DIALOG_TYPE: String = "dialog_type"

        fun newInstance(localTime: LocalTime, dialogType: DialogType): TimePickerFragment {
            return TimePickerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_LOCAL_TIME, localTime)
                    putSerializable(ARG_DIALOG_TYPE, dialogType)
                }
            }
        }
    }
}