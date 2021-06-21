package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ApplyChangesDialogCallbacks

class ApplyChangesDialog: DialogFragment() {

    private var callbacks: ApplyChangesDialogCallbacks? = null

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = activity as ApplyChangesDialogCallbacks
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i("WorkingDaysPickerDialog", "onCreateDialog()")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
                    .setTitle("You haven't saved changes")
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                callbacks?.onApply()
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                callbacks?.onDismiss()
                            })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}