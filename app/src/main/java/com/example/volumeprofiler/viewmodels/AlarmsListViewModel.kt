package com.example.volumeprofiler.viewmodels
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch

class AlarmsListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    val eventListLiveData: LiveData<List<AlarmTrigger>>
        get() {
            return repository.observeProfilesWithAlarms()
        }

    fun removeAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.removeAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }
}