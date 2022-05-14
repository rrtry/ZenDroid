package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
): ViewModel() {

    val alarmsFlow: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()

    sealed class ViewEvent {

        data class OnAlarmRemoved(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()
        data class OnAlarmCancelled(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()
        data class OnAlarmSet(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()

    }

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEvents: Flow<ViewEvent> = channel.receiveAsFlow()

    fun scheduleAlarm(alarmRelation: AlarmRelation) {
        viewModelScope.launch {
            scheduleAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmSet(alarmRelation, getScheduledAlarms()))
        }
    }

    fun cancelAlarm(alarmRelation: AlarmRelation) {
        viewModelScope.launch {
            cancelAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmCancelled(alarmRelation, getScheduledAlarms()))
        }
    }

    fun removeAlarm(alarmRelation: AlarmRelation) {
        viewModelScope.launch {
            removeAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmRemoved(alarmRelation, getScheduledAlarms()))
        }
    }

    private suspend fun getScheduledAlarms(): List<AlarmRelation> {
        return alarmRepository.getEnabledAlarms() ?: listOf()
    }

    private suspend fun scheduleAlarm(alarm: Alarm) {
        alarm.isScheduled = true
        alarmRepository.updateAlarm(alarm)
    }

    private suspend fun cancelAlarm(alarm: Alarm) {
        alarm.isScheduled = false
        alarmRepository.updateAlarm(alarm)
    }

    private suspend fun removeAlarm(alarm: Alarm) {
        alarmRepository.removeAlarm(alarm)
    }
}