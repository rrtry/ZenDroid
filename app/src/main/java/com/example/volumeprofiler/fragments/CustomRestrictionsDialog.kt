package com.example.volumeprofiler.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager.Policy.*
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.view.get
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.CustomPolicyDialogCallbacks

class CustomRestrictionsDialog: DialogFragment() {

    private var selectedItems: ArrayList<Int> = arrayListOf()
    private var suppressedEffectsMap: ArrayMap<List<Int>, Int> = arrayMapOf(
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_FULL_SCREEN_INTENT) to 0,
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_AMBIENT) to 1,
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_BADGE) to 2,
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_STATUS_BAR) to 3,
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_SCREEN_ON, SUPPRESSED_EFFECT_PEEK) to 4,
        listOf(SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_STATUS_BAR, SUPPRESSED_EFFECT_NOTIFICATION_LIST) to 5
    )

    private var callbacks: CustomPolicyDialogCallbacks? = null

    @SuppressWarnings("unchecked")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
    }

    override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (selectedItems.isEmpty()) {
            val suppressedEffects: List<Int> = arguments?.get(EXTRA_POLICY_SETTINGS) as List<Int>
            if (suppressedEffects.isNotEmpty()) {
                for (list in suppressedEffectsMap.keys) {
                    if (suppressedEffects.containsAll(list)) {
                        val index: Int? = suppressedEffectsMap[list]
                        selectedItems.add(index!!)
                        alertDialog.listView.setItemChecked(index, true)
                    }
                }
            }
        }
        else {
            for (value in selectedItems) {
                alertDialog.listView.setItemChecked(value, true)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = targetFragment as CustomPolicyDialogCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun constructString(): String {
        val stringBuilder: StringBuilder = java.lang.StringBuilder()
        for (i in selectedItems) {
            val key: String = suppressedEffectsMap.filterValues { it == i }.keys.flatten().joinToString("")
            stringBuilder.append(key)
        }
        return stringBuilder.toString()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.customRestrictions)
                    .setMultiChoiceItems(R.array.restrictions, null,
                            DialogInterface.OnMultiChoiceClickListener { dialog, which, isChecked ->
                                val alertDialog: AlertDialog  = dialog as AlertDialog
                                if (isChecked) {
                                    if (which == 5) {
                                        alertDialog.listView.setItemChecked(3, true)
                                        selectedItems.add(3)
                                        val item = alertDialog.listView[3]
                                        item.setOnClickListener(null)
                                    }
                                    selectedItems.add(which)
                                }
                                else if (selectedItems.contains(which)) {
                                    if (which == 5) {
                                        val item = alertDialog.listView.get(3)
                                        item.isEnabled = true
                                    }
                                    selectedItems.remove(Integer.valueOf(which))
                                }
                            })
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                callbacks?.onPolicySelected(constructString())
                                dialog.dismiss()
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                dialog.dismiss()
                            })



            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        private const val VIEW_TYPE_DISABLED: Int = 0
        private const val VIEW_TYPE_ENABLED: Int = 1
        const val EXTRA_POLICY_SETTINGS: String = "extra_policy_settings"
        private const val EXTRA_SELECTED_ITEMS: String = "extra_selected_items"

        fun newInstance(): CustomRestrictionsDialog {
            val args: Bundle = Bundle().apply {
                this.putIntegerArrayList(EXTRA_POLICY_SETTINGS, arrayListOf())
            }
            return CustomRestrictionsDialog().apply {
                this.arguments = args
            }
        }
    }
}