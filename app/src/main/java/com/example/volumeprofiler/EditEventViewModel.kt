package com.example.volumeprofiler

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

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

    fun selectEvent(id: Long): Unit {
        eventIdLiveData.value = id
    }

    fun selectMutableEvent(event: Event) {
        mutableEvent.value = event
    }

    override fun onCleared(): Unit {
        Log.i("EditEventViewModel", "onCleared()")
    }
}