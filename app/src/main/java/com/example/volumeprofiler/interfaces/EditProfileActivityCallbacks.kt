package com.example.volumeprofiler.interfaces

interface EditProfileActivityCallbacks {

    fun onFragmentReplace(fragment: Int): Unit

    fun onPopBackStack(): Unit
}