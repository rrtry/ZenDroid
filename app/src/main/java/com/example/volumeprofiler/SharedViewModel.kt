package com.example.volumeprofiler

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel: ViewModel() {

    var isProfileQueryEmpty: MutableLiveData<Boolean> = MutableLiveData()

    fun setValue(value: Boolean) {
        isProfileQueryEmpty.value = value
    }

    fun getValue(): Boolean? = isProfileQueryEmpty.value
}