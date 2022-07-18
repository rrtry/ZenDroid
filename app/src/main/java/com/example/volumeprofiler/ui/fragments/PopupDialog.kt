package com.example.volumeprofiler.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class PopupDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            requireArguments().let { args ->
                builder.setTitle(args.getString(EXTRA_TITLE))
                    .setMessage(args.getString(EXTRA_MESSAGE))
                    .setPositiveButton("Dismiss") { dialog, id ->  dialog.cancel() }
                    .setIcon(args.getInt(EXTRA_ICON_RES))
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        private const val EXTRA_TITLE: String = "title"
        private const val EXTRA_MESSAGE: String = "message"
        private const val EXTRA_ICON_RES: String = "icon"

        fun create(title: String, message: String, icon: Int): PopupDialog {
            return PopupDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_MESSAGE, message)
                    putInt(EXTRA_ICON_RES, icon)
                }
            }
        }
    }
}