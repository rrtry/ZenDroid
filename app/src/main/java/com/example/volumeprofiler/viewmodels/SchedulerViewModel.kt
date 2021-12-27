package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.EventRepository
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.EventRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val eventRepository: EventRepository
): ViewModel() {

    val alarmsFlow: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()
    val eventsFlow: Flow<List<EventRelation>> = eventRepository.observeEvents()

    fun addEvent(event: Event): Unit {
        viewModelScope.launch {
            eventRepository.insertEvent(event)
        }
    }

    fun updateEvent(event: Event): Unit {
        viewModelScope.launch {
            eventRepository.updateEvent(event)
        }
    }

    private fun removeEvent(event: Event): Unit {
        viewModelScope.launch {
            eventRepository.deleteEvent(event)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }

    fun addAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            alarmRepository.addAlarm(alarm)
        }
    }

    private fun removeAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            alarmRepository.removeAlarm(alarm)
        }
    }

    fun cancelAlarm(alarmRelation: AlarmRelation): Unit {
        val alarm: Alarm = alarmRelation.alarm
        alarm.isScheduled = 0
        updateAlarm(alarm)
    }

    fun cancelEvent(eventRelation: EventRelation): Unit {
        val event: Event = eventRelation.event
        event.scheduled = false
        updateEvent(event)
    }

    fun scheduleAlarm(alarmRelation: AlarmRelation): Unit {
        val alarm: Alarm = alarmRelation.alarm
        alarm.isScheduled = 1
        updateAlarm(alarm)
    }

    fun scheduleEvent(eventRelation: EventRelation): Unit {
        val event: Event = eventRelation.event
        event.scheduled = true
        updateEvent(event)
    }

    fun removeAlarm(alarmRelation: AlarmRelation) {
        val alarm: Alarm = alarmRelation.alarm
        removeAlarm(alarm)
    }

    fun removeEvent(eventRelation: EventRelation): Unit {
        val event: Event = eventRelation.event
        removeEvent(event)
    }
}