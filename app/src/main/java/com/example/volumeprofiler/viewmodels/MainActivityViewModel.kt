package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {

    sealed class ViewEvent {

        data class UpdateFloatingActionButton(val fab: FloatingActionButton, val fragment: Int): ViewEvent()
        data class OnSwiped(val fragment: Int): ViewEvent()
        data class OnFloatingActionButtonClick(val fab: FloatingActionButton, val fragment: Int): ViewEvent()
        data class OnMenuOptionSelected(val itemId: Int): ViewEvent()
    }

    val viewEvents: MutableSharedFlow<ViewEvent> = MutableSharedFlow(replay = 1)
    val showDialog: MutableStateFlow<Boolean> = MutableStateFlow(true)

    fun onMenuOptionSelected(itemId: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.OnMenuOptionSelected(itemId))
        }
    }

    fun updateFloatingActionButton(fab: FloatingActionButton, fragment: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.UpdateFloatingActionButton(fab, fragment))
        }
    }

    fun onFragmentSwiped(fragment: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.OnSwiped(fragment))
        }
    }

    fun onFloatingActionButtonClicked(fab: FloatingActionButton, fragment: Int) {
        viewModelScope.launch {
            viewEvents.emit(ViewEvent.OnFloatingActionButtonClick(fab, fragment))
        }
    }
}