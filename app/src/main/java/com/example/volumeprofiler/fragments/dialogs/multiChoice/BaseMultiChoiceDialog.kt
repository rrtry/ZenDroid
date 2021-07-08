package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import java.util.*
import kotlin.NumberFormatException
import kotlin.collections.ArrayList

abstract class BaseMultiChoiceDialog <T>: DialogFragment() {

    protected var selectedItems: ArrayList<Int> = arrayListOf()

    @get:ArrayRes
    abstract val arrayRes: Int
    abstract val optionsMap: ArrayMap<Int, T>
    abstract val title: String

    abstract fun onApply(string: String): Unit

    protected open fun constructString(): String {
        val stringBuilder: StringBuilder = java.lang.StringBuilder()
        for (i in selectedItems) {
            stringBuilder.append("${optionsMap[i]},")
        }
        return stringBuilder.toString()
    }

    protected open fun getKey(value: String): Int? {
        var result: Int? = null
        try {
            val num: Int = value.toInt()
            for ((key, value1) in optionsMap.entries) {
                if (Objects.equals(num, value1)) {
                    result = key
                    break
                }
            }
        }
        catch (e: NumberFormatException) {
            Log.d("BaseDialog", "NumberFormatException", e)
        }
        return result
    }

    @SuppressWarnings("unchecked")
    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
        }
    }

    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
    }

    final override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (selectedItems.isEmpty()) {
            val args: List<String> = (arguments?.getString(ARG_SELECTED_ITEMS) as String).split(",")
            if (args.isNotEmpty()) {
                for ((index, value) in args.withIndex()) {
                    val key: Int? = getKey(value)
                    if (key != null) {
                        selectedItems.add(key)
                        alertDialog.listView.setItemChecked(key, true)
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

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(title)
                    .setMultiChoiceItems(arrayRes, null,
                            DialogInterface.OnMultiChoiceClickListener { dialog, which, isChecked ->
                                if (isChecked) {
                                    selectedItems.add(which)
                                }
                                else if (selectedItems.contains(which)) {
                                    selectedItems.remove(which)
                                }
                            })
                    .setPositiveButton(R.string.apply,
                            DialogInterface.OnClickListener { dialog, id ->
                                onApply(constructString())
                            })
                    .setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                dialog.dismiss()
                            })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        const val EXTRA_SELECTED_ITEMS: String = "extra_selected_items"
        const val ARG_SELECTED_ITEMS: String = "items_selected_items"
    }
}