package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {

    sealed class ViewEvent {
        data class AnimateFloatingActionButton(val fragment: Int): ViewEvent()
        data class OnSwiped(val fragment: Int): ViewEvent()
        data class OnFloatingActionButtonClick(val fragment: Int): ViewEvent()
        data class OnMenuOptionSelected(val itemId: Int): ViewEvent()
    }

    val viewEvents: MutableSharedFlow<ViewEvent?> = MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val animateFab: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showDialog: MutableStateFlow<Boolean> = MutableStateFlow(true)

    fun onMenuOptionSelected(itemId: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.OnMenuOptionSelected(itemId))
        }
    }

    fun animateFloatingActionButton(fragment: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.AnimateFloatingActionButton(fragment))
        }
    }

    fun onFragmentSwiped(fragment: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.OnSwiped(fragment))
        }
    }

    fun onFloatingActionButtonClicked(fragment: Int) {
        viewModelScope.launch {
            animateFab.value = false
            viewEvents.emit(ViewEvent.OnFloatingActionButtonClick(fragment))
        }
    }
}