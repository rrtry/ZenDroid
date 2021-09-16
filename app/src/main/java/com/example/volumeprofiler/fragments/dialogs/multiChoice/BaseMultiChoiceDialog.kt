package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R
import java.util.*
import kotlin.collections.ArrayList

abstract class BaseMultiChoiceDialog <T>: DialogFragment() {

    protected var selectedItems: ArrayList<Int> = arrayListOf()
    private var shouldSetArgs: Boolean = false

    @get:ArrayRes
    abstract val arrayRes: Int
    abstract val optionsMap: ArrayMap<Int, T>
    abstract val title: String

    abstract fun onApply(arrayList: ArrayList<Int>): Unit

    protected open fun getArrayList(): ArrayList<Int> {
        val arrayList: ArrayList<Int> = arrayListOf()
        for (i in selectedItems) {
            val value: Int = optionsMap[i] as Int
            arrayList.add(value)
        }
        return arrayList
    }

    protected open fun getKey(value: Int): Int? {
        var result: Int? = null
        for ((key, value1) in optionsMap.entries) {
            if (Objects.equals(value, value1)) {
                result = key
                break
            }
        }
        return result
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            @Suppress("unchecked_cast")
            selectedItems = savedInstanceState.getSerializable(EXTRA_SELECTED_ITEMS) as ArrayList<Int>
            shouldSetArgs = savedInstanceState.getBoolean(EXTRA_LOADED_ARGS)
        }
    }

    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_SELECTED_ITEMS, selectedItems)
        outState.putBoolean(EXTRA_LOADED_ARGS, shouldSetArgs)
    }

    @Suppress("unchecked_cast")
    final override fun onResume() {
        super.onResume()
        val alertDialog: AlertDialog = dialog as AlertDialog
        if (!shouldSetArgs) {
            val args: ArrayList<Int> = arguments?.getSerializable(ARG_SELECTED_ITEMS) as ArrayList<Int>
            if (args.isNotEmpty()) {
                for ((index, value) in args.withIndex()) {
                    val key: Int? = getKey(value)
                    if (key != null) {
                        selectedItems.add(key)
                        alertDialog.listView.setItemChecked(key, true)
                    }
                }
            }
            shouldSetArgs = true
        }
        else {
            if (selectedItems.isNotEmpty()) {
                for (value in selectedItems) {
                    alertDialog.listView.setItemChecked(value, true)
                }
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
                    .setPositiveButton(R.string.apply
                    ) { _, _ ->
                        onApply(getArrayList())
                    }
                    .setNegativeButton(R.string.cancel
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {

        private const val EXTRA_LOADED_ARGS: String = "extra_loaded_args"
        const val EXTRA_SELECTED_ITEMS: String = "extra_selected_items"
        const val ARG_SELECTED_ITEMS: String = "items_selected_items"
    }
}