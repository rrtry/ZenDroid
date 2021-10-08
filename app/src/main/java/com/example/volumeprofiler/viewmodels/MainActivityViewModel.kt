package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
        private val repository: ProfileRepository
): ViewModel() {

    val showDialog: MutableStateFlow<Boolean> = MutableStateFlow(true)
}