package com.example.volumeprofiler.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.activities.AlarmDetailsActivity.Companion.TIME_REQUEST_KEY
import java.time.LocalTime

class TimePickerFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments?.getSerializable(ARG_LOCAL_TIME)
        val localTime = if (arguments != null) {
            args as LocalTime
        }
        else {
            LocalTime.now()
        }
        return TimePickerDialog(context, this, localTime.hour, localTime.minute, DateFormat.is24HourFormat(context))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val adjusted: LocalTime = LocalTime.of(hourOfDay, minute)
        setResult(adjusted)
    }

    private fun setResult(adjustedLocalTime: LocalTime): Unit {
        parentFragmentManager.setFragmentResult(TIME_REQUEST_KEY, Bundle().apply {
            putSerializable(EXTRA_LOCAL_TIME, adjustedLocalTime)
        })
    }

    companion object {

        private const val ARG_LOCAL_TIME = "arg_local_time"
        const val EXTRA_LOCAL_TIME = "extra_local_date_time"

        fun newInstance(localTime: LocalTime): TimePickerFragment {
            val args = Bundle().apply {
                this.putSerializable(ARG_LOCAL_TIME, localTime)
            }
            return TimePickerFragment().apply {
                this.arguments = args
            }
        }
    }
}