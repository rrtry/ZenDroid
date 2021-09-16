package com.example.volumeprofiler.interfaces

import com.example.volumeprofiler.databinding.CreateProfileActivityBinding

interface EditProfileActivityCallbacks {

    fun onFragmentReplace(fragment: Int): Unit

    fun onPopBackStack(): Unit
}