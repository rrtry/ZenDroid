package com.example.volumeprofiler.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.activities.EditAlarmActivity.Companion.TIME_REQUEST_KEY
import java.time.LocalDateTime

class TimePickerFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Context = requireContext()
        val args = arguments?.getSerializable(ARG_LOCAL_DATE_TIME)
        val localDateTime = if (arguments != null) {
            args as LocalDateTime
        }
        else {
            LocalDateTime.now()
        }
        return TimePickerDialog(context, this, localDateTime.hour, localDateTime.minute, DateFormat.is24HourFormat(context))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val localDateTime: LocalDateTime = LocalDateTime.now()
        val adjusted: LocalDateTime = localDateTime.withHour(hourOfDay).withMinute(minute).withSecond(0)
        setResult(adjusted)
    }

    private fun setResult(adjustedLocalDateTime: LocalDateTime): Unit {
        parentFragmentManager.setFragmentResult(TIME_REQUEST_KEY, Bundle().apply {
            this.putSerializable(EXTRA_LOCAL_DATE_TIME, adjustedLocalDateTime)
        })
    }

    companion object {

        private const val ARG_LOCAL_DATE_TIME = "arg_local_date_time"
        const val EXTRA_LOCAL_DATE_TIME = "extra_local_date_time"

        fun newInstance(date: LocalDateTime): TimePickerFragment {
            val args = Bundle().apply {
                this.putSerializable(ARG_LOCAL_DATE_TIME, date)
            }
            return TimePickerFragment().apply {
                this.arguments = args
            }
        }
    }
}