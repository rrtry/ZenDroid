package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R

class NoProfilesDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("No profiles were found")
                .setMessage("You need to create at least one profile in order to schedule events")
                .setPositiveButton("Dismiss") {
                        dialog, id ->  dialog.cancel()
                }
                .setIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}