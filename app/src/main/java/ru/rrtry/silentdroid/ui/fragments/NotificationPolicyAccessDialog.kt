package ru.rrtry.silentdroid.ui.fragments

import android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.util.getCategoryName
import ru.rrtry.silentdroid.util.openNotificationPolicySettingsActivity

class NotificationPolicyAccessDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)

            builder.setTitle(getCategoryName(requireContext(), ACCESS_NOTIFICATION_POLICY))
                .setCancelable(false)
                .setMessage(requireContext().getString(R.string.interruption_policy_access_explanation))
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    context?.openNotificationPolicySettingsActivity()
                    dismiss()
                }
                .setNegativeButton(resources.getString(R.string.close)) { dialog, id ->
                    requireActivity().finish()
                }
                .setIcon(R.drawable.ic_baseline_do_not_disturb_on_total_silence_24)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}