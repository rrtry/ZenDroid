package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.database.repositories.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.text.FieldPosition
import javax.inject.Inject

class MainActivityViewModel: ViewModel() {

    val showDialog: MutableStateFlow<Boolean> = MutableStateFlow(true)
}