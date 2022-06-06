package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.core.ScheduleCalendar
import com.example.volumeprofiler.core.WeekDay
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
): ViewModel() {

    sealed class ViewEvent {

        data class OnAlarmRemoved(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()
        data class OnAlarmCancelled(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()
        data class OnAlarmSet(val relation: AlarmRelation, val scheduledAlarms: List<AlarmRelation>): ViewEvent()

    }

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEvents: Flow<ViewEvent> = channel.receiveAsFlow()
    val alarmsFlow: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()

    fun scheduleAlarm(relation: AlarmRelation) {
        viewModelScope.launch {
            if (!ScheduleCalendar.isValid(ZonedDateTime.now(), relation.alarm)) {
                ScheduleCalendar.getStartAndEndDate(relation.alarm.startTime, relation.alarm.endTime).let { date ->
                    relation.alarm = relation.alarm.apply {
                        startDateTime = date.first
                        endDateTime = date.second
                    }
                    alarmRepository.updateAlarm(relation.alarm)
                }
            }
            scheduleAlarm(relation.alarm)
            channel.send(ViewEvent.OnAlarmSet(relation, getScheduledAlarms()))
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