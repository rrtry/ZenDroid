package com.example.volumeprofiler.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.interfaces.TimePickerFragmentCallback
import java.time.LocalDateTime

class TimePickerFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private var callback: TimePickerFragmentCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = activity as TimePickerFragmentCallback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
        val localDateTime: LocalDateTime = LocalDateTime.now()
        val adjusted: LocalDateTime = localDateTime.withHour(hourOfDay).withMinute(minute).withSecond(0)
        callback?.onTimeSelected(adjusted)
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