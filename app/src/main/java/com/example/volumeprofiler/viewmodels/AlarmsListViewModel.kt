package com.example.volumeprofiler.viewmodels
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.repositories.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsListViewModel @Inject constructor(
        private val repository: AlarmRepository
): ViewModel() {

    val alarmListLiveData: LiveData<List<AlarmTrigger>> = repository.observeAlarmTriggers().asLiveData()

    private fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun addAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            repository.addAlarm(alarm)
        }
    }

    private fun removeAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            repository.removeAlarm(alarm)
        }
    }

    fun cancelAlarm(alarmTrigger: AlarmTrigger): Unit {
        val alarm: Alarm = alarmTrigger.alarm
        alarm.isScheduled = 0
        updateAlarm(alarm)
    }

    fun scheduleAlarm(alarmTrigger: AlarmTrigger): Unit {
        val alarm: Alarm = alarmTrigger.alarm
        alarm.isScheduled = 1
        updateAlarm(alarm)
    }

    fun removeAlarm(alarmTrigger: AlarmTrigger) {
        val alarm: Alarm = alarmTrigger.alarm
        removeAlarm(alarm)
    }
}