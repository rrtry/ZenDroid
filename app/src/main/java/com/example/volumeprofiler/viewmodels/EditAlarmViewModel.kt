package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch

class EditAlarmViewModel: ViewModel() {

    private val repository: Repository = Repository.get()

    var selectedItem: Int = -1
    val profileListLiveData: LiveData<List<Profile>> = repository.observeProfiles().asLiveData()

    var mutableAlarm: Alarm? = null
    var mutableProfile: Profile? = null

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.addAlarm(alarm)
        }
    }

    override fun onCleared(): Unit {
        Log.i("EditEventViewModel", "onCleared()")
    }
}