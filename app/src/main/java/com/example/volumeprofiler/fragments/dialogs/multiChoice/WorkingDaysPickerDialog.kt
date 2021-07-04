package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.util.Log
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.DaysPickerDialogCallback
import kotlin.collections.ArrayList

class WorkingDaysPickerDialog: DialogFragment() {

    private var selectedItems: ArrayList<Int> = arrayListOf()
    private var callbacks: DaysPickerDialogCallback? = null

    @SuppressWarnings("unchecked")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = activity as DaysPickerDialogCallback
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
    }

    override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (selectedItems.isEmpty()) {
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
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.workingDaysTitle)
                    .setMultiChoiceItems(R.array.daysOfWeek, null,
                            DialogInterface.OnMultiChoiceClickListener { dialog, which, isChecked ->
                                if (isChecked) {
                                    selectedItems.add(which)
                                }
                                else if (selectedItems.contains(which)) {
                                    selectedItems.remove(Integer.valueOf(which))
                                }
                            })
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                callbacks?.onDaysSelected(selectedItems)
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                dialog.dismiss()
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