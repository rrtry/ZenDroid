package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ApplyChangesDialogCallbacks

class ApplyChangesDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i("WorkingDaysPickerDialog", "onCreateDialog()")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
                    .setTitle("You haven't saved changes")
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                (activity as ApplyChangesDialogCallbacks).onApply()
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                (activity as ApplyChangesDialogCallbacks).onDismiss()
                            })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}