package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.AlarmDetailsActivity
import kotlin.collections.ArrayList

class ScheduledDaysPickerDialog: DialogFragment() {

    private var selectedItems: ArrayList<Int> = arrayListOf()
    private var shouldSetArgs: Boolean = true

    @SuppressWarnings("unchecked")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
            shouldSetArgs = savedInstanceState.getBoolean(EXTRA_SHOULD_SET_ARGS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
        outState.putBoolean(EXTRA_SHOULD_SET_ARGS, shouldSetArgs)
    }

    override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (shouldSetArgs) {
            val workingDays: ArrayList<Int> = arguments?.getSerializable(ARG_SCHEDULED_DAYS) as ArrayList<Int>
            if (workingDays.isNotEmpty()) {
                for (value in workingDays) {
                    val index: Int = value - 1
                    selectedItems.add(index)
                    alertDialog.listView.setItemChecked(index, true)
                }
            }
            shouldSetArgs = false
        }
        else {
            if (selectedItems.isNotEmpty()) {
                for (value in selectedItems) {
                    alertDialog.listView.setItemChecked(value, true)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.workingDaysTitle)
                    .setMultiChoiceItems(R.array.daysOfWeek, null
                    ) { _, which, isChecked ->
                        if (isChecked) {
                            selectedItems.add(which)
                        } else if (selectedItems.contains(which)) {
                            selectedItems.remove(Integer.valueOf(which))
                        }
                    }
                    .setPositiveButton(R.string.apply
                    ) { _, id ->
                        setSuccessfulResult()
                    }
                    .setNegativeButton(R.string.cancel
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setSuccessfulResult(): Unit {
        parentFragmentManager.setFragmentResult(AlarmDetailsActivity.SCHEDULED_DAYS_REQUEST_KEY, Bundle().apply {
            val list: ArrayList<Int> = selectedItems.map { it + 1 } as ArrayList<Int>
            list.sort()
            this.putSerializable(EXTRA_SCHEDULED_DAYS, list)
        })
    }

    companion object {

        fun newInstance(scheduledDays: ArrayList<Int>): ScheduledDaysPickerDialog {
            val arguments: Bundle = Bundle().apply {
                this.putSerializable(ARG_SCHEDULED_DAYS, scheduledDays)
            }
            return ScheduledDaysPickerDialog().apply {
                this.arguments = arguments
            }
        }

        const val EXTRA_SCHEDULED_DAYS: String = "extra_scheduled_days"
        private const val ARG_SCHEDULED_DAYS: String = "arg_working_days"
        private const val EXTRA_SELECTED_ITEMS: String = "extra_selected_items"
        private const val EXTRA_SHOULD_SET_ARGS: String = "extra_should_set_args"
    }
}