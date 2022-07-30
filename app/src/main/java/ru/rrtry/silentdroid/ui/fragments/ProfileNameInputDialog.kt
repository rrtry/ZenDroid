package ru.rrtry.silentdroid.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import ru.rrtry.silentdroid.databinding.TextInputDialogBinding

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
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        bindingImpl = TextInputDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textInputEditText.setText(viewModel.title.value)
        binding.positiveButton.setOnClickListener {
            binding.textInputEditText.text.toString().also { title ->
                viewModel.title.value = title.trim().ifBlank { resources.getString(R.string.no_title) }
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