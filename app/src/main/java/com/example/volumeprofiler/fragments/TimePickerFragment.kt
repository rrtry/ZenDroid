package com.example.volumeprofiler.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.interfaces.TimePickerFragmentCallbacks
import java.time.LocalDateTime

class TimePickerFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private var callbacks: TimePickerFragmentCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = activity as TimePickerFragmentCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i("TimePickerDialog", "onCreateDialog()")
        val context: Context = requireContext()
        val args = arguments?.getSerializable(ARG_DATE)
        val localDateTime = if (arguments != null) {
            args as LocalDateTime
        }
        else {
            LocalDateTime.now()
        }
        return TimePickerDialog(context, this, localDateTime.hour, localDateTime.minute, DateFormat.is24HourFormat(context))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        Log.i("TimePickerDialog", "onTimeSet(), hourOfDay: $hourOfDay, minute: $minute")
        val localDateTime: LocalDateTime = LocalDateTime.now()
        val adjusted: LocalDateTime = localDateTime.withHour(hourOfDay).withMinute(minute).withSecond(0)
        callbacks?.onTimeSelected(adjusted)
    }

    companion object {

        private const val ARG_DATE = "arg_date"

        fun newInstance(date: LocalDateTime): TimePickerFragment {
            val args = Bundle().apply {
                this.putSerializable(ARG_DATE, date)
            }
            return TimePickerFragment().apply {
                this.arguments = args
            }
        }
    }
}