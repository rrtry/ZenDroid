package com.example.volumeprofiler.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ProfileNameInputDialogCallbacks

class ProfileNameInputDialog: DialogFragment() {

    private var callbacks: ProfileNameInputDialogCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = targetFragment as ProfileNameInputDialogCallbacks
    }
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.custom_dialog_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCallbacks(view)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        inflateLayout(builder)
        return builder.show()
    }

    private fun setCallbacks(view: View): Unit {
        val positiveButton: Button = view.findViewById(R.id.positiveButton)
        val negativeButton: Button = view.findViewById(R.id.negativeButton)
        val editText: EditText = view.findViewById(R.id.textInputEditText)

        editText.text = SpannableStringBuilder(arguments?.getString(EXTRA_TITLE))

        positiveButton.setOnClickListener {
            callbacks?.onApply(editText.text.toString())
            finish()
        }
        negativeButton.setOnClickListener {
            finish()
        }
    }

    private fun finish(): Unit {
        dialog?.dismiss()
    }

    private fun inflateLayout(builder: AlertDialog.Builder): Unit {
        val layoutInflater: LayoutInflater = layoutInflater
        val view: View = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        builder.setView(view)
    }

    companion object {

        const val TAG: String = "ProfileNameInputDialogTag"
        const val EXTRA_TITLE: String = "extra_title"

        fun newInstance(title: String): ProfileNameInputDialog {
            val bundle: Bundle = Bundle().apply {
                this.putString(EXTRA_TITLE, title)
            }
            return ProfileNameInputDialog().apply {
                this.arguments = bundle
            }
        }
    }
}