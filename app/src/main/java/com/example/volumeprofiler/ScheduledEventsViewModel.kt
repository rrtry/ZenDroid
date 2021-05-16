package com.example.volumeprofiler
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ScheduledEventsViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    val eventListLiveData: LiveData<List<ProfileAndEvent>>
        get() {
            return repository.observeProfilesWithEvents()
        }

    fun removeEvent(event: Event) {
        viewModelScope.launch {
            repository.removeEvent(event)
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }
}