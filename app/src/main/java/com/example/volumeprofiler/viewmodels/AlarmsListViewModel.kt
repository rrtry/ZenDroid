package com.example.volumeprofiler.viewmodels
import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsListViewModel @Inject constructor(
        private val repository: AlarmRepository
): ViewModel() {

    val alarmsFlow: Flow<List<AlarmRelation>> = repository.observeAlarms()

    fun updateAlarm(alarm: Alarm) {
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

    fun cancelAlarm(alarmRelation: AlarmRelation): Unit {
        val alarm: Alarm = alarmRelation.alarm
        alarm.isScheduled = 0
        updateAlarm(alarm)
    }

    fun scheduleAlarm(alarmRelation: AlarmRelation): Unit {
        val alarm: Alarm = alarmRelation.alarm
        alarm.isScheduled = 1
        updateAlarm(alarm)
    }

    fun removeAlarm(alarmRelation: AlarmRelation) {
        val alarm: Alarm = alarmRelation.alarm
        removeAlarm(alarm)
    }
}