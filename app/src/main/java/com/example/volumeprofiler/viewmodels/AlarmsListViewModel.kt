package com.example.volumeprofiler.viewmodels
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.util.AlarmUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AlarmsListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    val eventListLiveData: LiveData<List<AlarmTrigger>> = repository.observeProfilesWithAlarms().asLiveData()

    private fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun unScheduleAlarm(alarmTrigger: AlarmTrigger): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        val alarm: Alarm = alarmTrigger.alarm
        alarm.isScheduled = 0
        updateAlarm(alarm)
        alarmUtil.cancelAlarm(alarmTrigger.alarm, alarmTrigger.profile)
    }

    fun scheduleAlarm(alarmTrigger: AlarmTrigger): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        val alarm: Alarm = alarmTrigger.alarm
        alarm.isScheduled = 1
        updateAlarm(alarm)
        alarmUtil.setAlarm(alarmTrigger.alarm, alarmTrigger.profile, false)
    }

    fun removeAlarm(alarmTrigger: AlarmTrigger) {
        val alarm: Alarm = alarmTrigger.alarm
        if (alarm.isScheduled == 1) {
            val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
            alarmUtil.cancelAlarm(alarm, alarmTrigger.profile)
        }
        viewModelScope.launch {
            repository.removeAlarm(alarm)
        }
    }
}