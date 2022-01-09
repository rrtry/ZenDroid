package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.volumeprofiler.R

abstract class BasePolicyPreferencesDialog: DialogFragment() {

    protected abstract val title: String
    protected abstract val arrayRes: Int
    protected abstract val categories: List<Int>

    protected var categoriesMask: Int = 0
    private var argsSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            categoriesMask = savedInstanceState.getInt(EXTRA_CATEGORIES, 0)
            argsSet = savedInstanceState.getBoolean(EXTRA_SET_ARGUMENTS, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_CATEGORIES, categoriesMask)
        outState.putBoolean(EXTRA_SET_ARGUMENTS, argsSet)
    }

    override fun onResume() {
        super.onResume()

        if (!argsSet) {
            categoriesMask = requireArguments().getInt(EXTRA_CATEGORIES, 0)
            argsSet = true
        }

        val listView: ListView = getListView()
        for ((index, category) in categories.withIndex()) {
            if ((categoriesMask and category) != 0) {
                listView.setItemChecked(index, true)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(title)
                .setMultiChoiceItems(arrayRes, null)
                { _, which, isChecked ->

                    val category: Int = categories[which]

                    if (isChecked) {
                        categoriesMask = categoriesMask or category
                    } else if ((categoriesMask and category) != 0) {
                        categoriesMask = categoriesMask and category.inv()
                    }
                    (getListView().adapter as ArrayAdapter<*>).notifyDataSetChanged()
                }
                .setPositiveButton(R.string.apply)
                { _, _ ->
                    applyChanges(categoriesMask)
                }
                .setNegativeButton(R.string.cancel)
                { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    protected fun getListView(): ListView {
        return (dialog as AlertDialog).listView
    }

    abstract fun applyChanges(mask: Int): Unit

    companion object {

        const val EXTRA_CATEGORIES: String = "extra_categories"
        const val EXTRA_SET_ARGUMENTS: String = "extra_set_arguments"

    }

}