package ru.rrtry.silentdroid.viewmodels

import androidx.lifecycle.*
import ru.rrtry.silentdroid.core.ScheduleCalendar
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
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
        object OnRequestExactAlarmPermission: ViewEvent()
    }

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEvents: Flow<ViewEvent> = channel.receiveAsFlow()
    val alarmsFlow: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()
    var canScheduleExactAlarms: Boolean = false

    fun scheduleAlarm(relation: AlarmRelation) {
        viewModelScope.launch {
            if (canScheduleExactAlarms) {
                ScheduleCalendar.getStartAndEndDate(relation.alarm.startTime, relation.alarm.endTime).let { date ->
                    relation.alarm.startDateTime = date.first
                    relation.alarm.endDateTime = date.second
                    alarmRepository.updateAlarm(relation.alarm)
                }
                alarmRepository.scheduleAlarm(relation.alarm)
                channel.send(ViewEvent.OnAlarmSet(relation, getScheduledAlarms()))
            } else {
                channel.send(ViewEvent.OnRequestExactAlarmPermission)
            }
        }
    }

    fun cancelAlarm(alarmRelation: AlarmRelation) {
        viewModelScope.launch {
            alarmRepository.cancelAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmCancelled(alarmRelation, getScheduledAlarms()))
        }
    }

    fun removeAlarm(alarmRelation: AlarmRelation) {
        viewModelScope.launch {
            alarmRepository.removeAlarm(alarmRelation.alarm)
            channel.send(ViewEvent.OnAlarmRemoved(alarmRelation, getScheduledAlarms()))
        }
    }

    private suspend fun getScheduledAlarms(): List<AlarmRelation> {
        return alarmRepository.getEnabledAlarms() ?: listOf()
    }
}