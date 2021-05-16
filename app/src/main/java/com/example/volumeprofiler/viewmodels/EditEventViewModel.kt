package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.Repository
import kotlinx.coroutines.launch

class EditEventViewModel: ViewModel() {

    private val repository: Repository = Repository.get()

    var selectedProfile: Int = 0
    val mutableEvent: MutableLiveData<Event> = MutableLiveData()

    private val eventIdLiveData = MutableLiveData<Long>()
    val eventLiveData: LiveData<Event> = Transformations.switchMap(eventIdLiveData) {
        t -> repository.observeEvent(t)
    }
    val profileListLiveData: LiveData<List<Profile>>
        get() {
            return repository.observeProfiles()
        }
    var profileAndEventLiveData: LiveData<ProfileAndEvent?> = Transformations.switchMap(eventIdLiveData) { eventId -> repository.observeScheduledEventWithProfile(eventId) }

    fun selectEvent(id: Long): Unit {
        eventIdLiveData.value = id
    }

    fun selectMutableEvent(event: Event) {
        mutableEvent.value = event
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun addEvent(event: Event) {
        Log.i("EditEventViewModel", "addEvent")
        viewModelScope.launch {
            repository.addEvent(event)
        }
    }

    override fun onCleared(): Unit {
        Log.i("EditEventViewModel", "onCleared()")
    }
}