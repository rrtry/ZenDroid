package com.example.volumeprofiler.interfaces

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.selection.SelectionTracker

interface ActionModeProvider<T> {

    fun onActionItemRemove(): Unit

    fun getTracker(): SelectionTracker<T>

    fun getFragmentActivity(): FragmentActivity

}