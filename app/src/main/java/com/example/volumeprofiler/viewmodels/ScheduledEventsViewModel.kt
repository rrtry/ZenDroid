package com.example.volumeprofiler.viewmodels
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.Repository
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