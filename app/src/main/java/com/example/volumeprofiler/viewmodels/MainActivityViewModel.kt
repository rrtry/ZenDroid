package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {

    sealed class ViewEvent {

        data class UpdateFloatingActionButton(val fragment: Int): ViewEvent()
        data class OnSwiped(val fragment: Int): ViewEvent()
        data class OnFloatingActionButtonClick(val fragment: Int): ViewEvent()
        data class OnMenuOptionSelected(val itemId: Int): ViewEvent()
    }

    private val eventsFlow: MutableSharedFlow<ViewEvent?> = MutableSharedFlow<ViewEvent?>(replay = 1)
    val viewEvents: MutableSharedFlow<ViewEvent?> = eventsFlow
    val showDialog: MutableStateFlow<Boolean> = MutableStateFlow(true)

    fun onMenuOptionSelected(itemId: Int) {
        viewModelScope.launch {
            eventsFlow.emit(ViewEvent.OnMenuOptionSelected(itemId))
        }
    }

    fun updateFloatingActionButton(fragment: Int) {
        viewModelScope.launch {
            eventsFlow.emit(ViewEvent.UpdateFloatingActionButton(fragment))
        }
    }

    fun onFragmentSwiped(fragment: Int) {
        viewModelScope.launch {
            eventsFlow.emit(ViewEvent.OnSwiped(fragment))
        }
    }

    fun onFloatingActionButtonClicked(fragment: Int) {
        viewModelScope.launch {
            eventsFlow.emit(ViewEvent.OnFloatingActionButtonClick(fragment))
        }
    }
}