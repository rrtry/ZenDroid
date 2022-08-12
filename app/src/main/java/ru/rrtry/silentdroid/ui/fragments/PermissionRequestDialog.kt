package ru.rrtry.silentdroid.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.util.getCategoryName
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel

class PermissionRequestDialog: DialogFragment() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    private fun getActionDescription(permission: String): String {
        return if (shouldShowRequestPermissionRationale(permission)) {
            resources.getString(R.string.grant)
        } else {
            resources.getString(R.string.open_settings)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)
            val icon: Int = requireArguments().getInt(EXTRA_ICON_RESOURCE)
            val permission: String = requireArguments().getString(EXTRA_PERMISSION, "")
            val permissionDisplayName: String = getCategoryName(requireContext(), permission)
            val message: String = resources.getString(requireArguments().getInt(EXTRA_MESSAGE_STRING_RESOURCE))

            builder.setTitle(permissionDisplayName)
                .setMessage(message)
                .setPositiveButton(getActionDescription(permission)) { _, _ ->
                    viewModel.requestPermission(permission)
                    dismiss()
                }
                .setNegativeButton(resources.getString(R.string.close)) { dialog, id ->
                    dismiss()
                }
                .setIcon(icon)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        fun newInstance(permission: String, message: Int, iconResource: Int): PermissionRequestDialog {
            val dialog: PermissionRequestDialog = PermissionRequestDialog()
            dialog.arguments = Bundle().apply {
                putInt(EXTRA_MESSAGE_STRING_RESOURCE, message)
                putInt(EXTRA_ICON_RESOURCE, iconResource)
                putString(EXTRA_PERMISSION, permission)
            }
            return dialog
        }

        const val EXTRA_MESSAGE_STRING_RESOURCE: String = "extra_message"
        const val EXTRA_ICON_RESOURCE: String = "extra_icon_resource"
        const val EXTRA_PERMISSION: String = "extra_permission"
    }
}