package com.example.volumeprofiler.fragments

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
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.INPUT_TITLE_REQUEST_KEY
import com.example.volumeprofiler.databinding.TextInputDialogBinding
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileNameInputDialog: DialogFragment() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    private var bindingImpl: TextInputDialogBinding? = null
    private val binding: TextInputDialogBinding
    get() = bindingImpl!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        AlertDialog.Builder(requireContext()).apply {
            setView(layoutInflater.inflate(R.layout.text_input_dialog, null))
            return show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        bindingImpl = TextInputDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.positiveButton.setOnClickListener {
            binding.textInputEditText.text.toString().also {
                viewModel.title.value = it.ifEmpty {
                    "No title"
                }
            }
            dismiss()
        }
        binding.negativeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }
}