package com.example.volumeprofiler.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity.Companion.INPUT_TITLE_REQUEST_KEY
import com.example.volumeprofiler.fragments.EditProfileFragment

class ProfileNameInputDialog: DialogFragment() {

    private var editText: EditText? = null

    override fun onDestroyView() {
        editText = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.text_input_dialog, container, false)
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
        editText = view.findViewById(R.id.textInputEditText)
        editText?.setOnKeyListener(object : View.OnKeyListener {

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (editText!!.text!!.isNotEmpty()) {
                        changeTitle()
                    }
                    return true
                }
                return false
            }
        })
        editText?.text = SpannableStringBuilder(arguments?.getString(EXTRA_TITLE))
        positiveButton.setOnClickListener {
            if (editText!!.text!!.isNotEmpty()) {
                changeTitle()
            }
        }
        negativeButton.setOnClickListener {
            finish()
        }
    }

    private fun changeTitle(): Unit {
        parentFragmentManager.setFragmentResult(INPUT_TITLE_REQUEST_KEY, Bundle().apply {
            this.putString(EXTRA_TITLE, editText?.text.toString())
        })
        finish()
    }

    private fun finish(): Unit {
        dialog?.dismiss()
    }

    private fun inflateLayout(builder: AlertDialog.Builder): Unit {
        val layoutInflater: LayoutInflater = layoutInflater
        val view: View = layoutInflater.inflate(R.layout.text_input_dialog, null)
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