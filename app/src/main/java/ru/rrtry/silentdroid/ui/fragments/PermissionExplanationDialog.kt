package ru.rrtry.silentdroid.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import ru.rrtry.silentdroid.util.getCategoryName

class PermissionExplanationDialog: DialogFragment() {

    private fun getActionDescription(permission: String): String {
        return if (shouldShowRequestPermissionRationale(permission)) {
            "Grant permission"
        } else {
            "Open settings"
        }
    }

    private fun setResult(result: Boolean) {
        val bundle: Bundle = Bundle().apply {
            putBoolean(EXTRA_RESULT_OK, result)
            putString(EXTRA_PERMISSION, requireArguments().getString(EXTRA_PERMISSION)!!)
            putBoolean(
                EXTRA_REQUEST_MULTIPLE_PERMISSIONS, requireArguments().getBoolean(
                EXTRA_REQUEST_MULTIPLE_PERMISSIONS
                ))
        }
        parentFragmentManager.setFragmentResult(PERMISSION_REQUEST_KEY, bundle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val permission: String = requireArguments().getString(EXTRA_PERMISSION)!!
            val permissionDisplayName: String = getCategoryName(permission)
            builder.setTitle("$permissionDisplayName permission is required")
                .setMessage(resources.getString(requireArguments().getInt(
                    EXTRA_MESSAGE_STRING_RESOURCE
                )))
                .setPositiveButton(getActionDescription(permission)) {
                        _, _ ->
                    setResult(true)
                }
                .setNegativeButton("Dismiss") {
                    dialog, id ->
                    setResult(false)
                }
                .setIcon(requireArguments().getInt(EXTRA_ICON_RESOURCE))
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        fun newInstance(permission: String, message: Int, iconResource: Int, requestMultiplePermissions: Boolean = false): PermissionExplanationDialog {
            val dialog: PermissionExplanationDialog = PermissionExplanationDialog()
            dialog.arguments = Bundle().apply {
                putInt(EXTRA_MESSAGE_STRING_RESOURCE, message)
                putString(EXTRA_PERMISSION, permission)
                putInt(EXTRA_ICON_RESOURCE, iconResource)
                putBoolean(EXTRA_REQUEST_MULTIPLE_PERMISSIONS, requestMultiplePermissions)
            }
            return dialog
        }

        const val EXTRA_MESSAGE_STRING_RESOURCE: String = "extra_message"
        const val EXTRA_ICON_RESOURCE: String = "extra_icon_resource"
        const val EXTRA_PERMISSION: String = "extra_permission"
        const val EXTRA_REQUEST_MULTIPLE_PERMISSIONS: String = "extra_request_multiple_permissions"
        const val PERMISSION_REQUEST_KEY: String = "permission_request_key"
        const val EXTRA_RESULT_OK: String = "extra_result_ok"
    }
}