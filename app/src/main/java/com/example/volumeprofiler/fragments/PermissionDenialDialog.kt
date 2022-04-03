package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R

class PermissionDenialDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Insufficient permissions")
                .setMessage(
                    resources.getString(
                        R.string.permission_denial_warning
                    )
                )
                .setNegativeButton("Dismiss") {
                        dialog, id ->
                    dialog.dismiss()
                }
                .setIcon(R.drawable.ic_baseline_perm_device_information_24)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}