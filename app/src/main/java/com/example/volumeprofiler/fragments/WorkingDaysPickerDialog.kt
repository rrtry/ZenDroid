package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.util.Log
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.DaysPickerDialogCallbacks
import kotlin.collections.ArrayList

class WorkingDaysPickerDialog: DialogFragment() {

    private var selectedItems: ArrayList<Int> = arrayListOf()

    interface Callbacks {

        fun onDaysSelected(arrayList: ArrayList<Int>): Unit

        fun onDismiss(): Unit
    }

    @SuppressWarnings("unchecked")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
    }

    override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (selectedItems.isEmpty()) {
            Log.i("WorkingDaysPickerDialog", "selectedItems array is empty, filling it with argument's data")
            val workingDays: Array<Int> = arguments?.get(ARG_WORKING_DAYS) as Array<Int>
            if (workingDays.isNotEmpty()) {
                for (value in workingDays) {
                    val index: Int = value - 1
                    selectedItems.add(index)
                    alertDialog.listView.setItemChecked(index, true)
                }
            }
        }
        else {
            Log.i("WorkingDaysPickerDialog", "selectedItems array is not empty")
            for (value in selectedItems) {
                alertDialog.listView.setItemChecked(value, true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("WorkingDaysPickerDialog", "onDestroy: selectedItems array: $selectedItems")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i("WorkingDaysPickerDialog", "onCreateDialog()")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.workingDaysTitle)
                    .setMultiChoiceItems(R.array.daysOfWeek, null,
                            DialogInterface.OnMultiChoiceClickListener { dialog, which, isChecked ->
                                if (isChecked) {
                                    selectedItems.add(which)
                                } else if (selectedItems.contains(which)) {
                                    selectedItems.remove(Integer.valueOf(which))
                                }
                            })
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                (activity as DaysPickerDialogCallbacks).onDaysSelected(selectedItems)
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->

                            })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        fun newInstance(event: Event): WorkingDaysPickerDialog {
            val arguments: Bundle = Bundle().apply {
                val workingDays: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
                this.putSerializable(ARG_WORKING_DAYS, workingDays)
            }
            return WorkingDaysPickerDialog().apply {
                this.arguments = arguments
            }
        }

        private const val ARG_WORKING_DAYS: String = "arg_working_days"
        private const val EXTRA_SELECTED_ITEMS: String = "extra_selected_items"
    }
}