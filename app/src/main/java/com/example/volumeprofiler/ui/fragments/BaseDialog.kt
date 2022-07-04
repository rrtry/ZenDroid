package com.example.volumeprofiler.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R

abstract class BaseDialog: DialogFragment() {

    protected abstract val title: String
    protected abstract val arrayRes: Int
    protected abstract val values: List<Int>

    protected var mask: Int = 0
    private var argsSet: Boolean = false

    abstract fun applyChanges(mask: Int)
    abstract fun onValueAdded(position: Int, value: Int)
    abstract fun onValueRemoved(position: Int, value: Int)

    private fun removeValue(category: Int) {
        mask = mask and category.inv()
    }

    private fun addValue(category: Int) {
        mask = mask or category
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.apply {
            mask = getInt(EXTRA_MASK, 0)
            argsSet = getBoolean(EXTRA_SET_ARGUMENTS, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_MASK, mask)
        outState.putBoolean(EXTRA_SET_ARGUMENTS, argsSet)
    }

    override fun onResume() {
        super.onResume()
        if (!argsSet) {
            mask = requireArguments().getInt(EXTRA_MASK, 0)
            argsSet = true
        }
        getListView().apply {
            values.forEachIndexed { index, category ->
                if ((mask and category) != 0) {
                    setItemChecked(index, true)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(title)
                .setMultiChoiceItems(arrayRes, null) { _, which, isChecked ->
                    values[which].also { category ->
                        if (isChecked) {
                            addValue(category)
                            onValueAdded(which, category)
                        } else if ((mask and category) != 0) {
                            removeValue(category)
                            onValueRemoved(which, category)
                        }
                        // notifyDataSetChanged()
                    }
                }
                .setPositiveButton(R.string.apply) { _, _ ->
                    applyChanges(mask)
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getListView(): ListView {
        return (dialog as AlertDialog).listView
    }

    private fun notifyDataSetChanged() {
        (getListView().adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    companion object {

        const val EXTRA_MASK: String = "extra_mask"
        const val EXTRA_SET_ARGUMENTS: String = "extra_set_arguments"

    }

}