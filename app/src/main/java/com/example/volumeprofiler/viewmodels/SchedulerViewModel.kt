package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.EventRepository
import com.example.volumeprofiler.entities.EventRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val eventRepository: EventRepository
): ViewModel() {

    val alarmsFlow: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()
    val eventsFlow: Flow<List<EventRelation>> = eventRepository.observeEvents()

    sealed class ViewEvent {

        data class OnAlarmRemoved(val relation: AlarmRelation): ViewEvent()
        data class OnAlarmCancelled(val relation: AlarmRelation): ViewEvent()
        data class OnAlarmSet(val relation: AlarmRelation): ViewEvent()

    }

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEvents: Flow<ViewEvent> = channel.receiveAsFlow()

    fun sendScheduleAlarmEvent(alarmRelation: AlarmRelation): Unit {
        viewModelScope.launch {
            scheduleAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmSet(alarmRelation))
        }
    }

    fun sendCancelAlarmEvent(alarmRelation: AlarmRelation): Unit {
        viewModelScope.launch {
            cancelAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmCancelled(alarmRelation))
        }
    }

    fun sendRemoveAlarmEvent(alarmRelation: AlarmRelation): Unit {
        viewModelScope.launch {
            removeAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmRemoved(alarmRelation))
        }
    }

    private suspend fun scheduleAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 1
        updateAlarm(alarm)
    }

    private suspend fun removeAlarm(alarm: Alarm): Unit {
        alarmRepository.removeAlarm(alarm)
    }

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        updateAlarm(alarm)
    }

    private suspend fun updateAlarm(alarm: Alarm): Unit {
        alarmRepository.updateAlarm(alarm)
    }
}