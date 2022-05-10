package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumeprofiler.core.ScheduleCalendar
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.WeekDay
import com.example.volumeprofiler.util.WeekDay.Companion.WEEKDAYS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AlarmDetailsViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
        private val alarmRepository: AlarmRepository
): ViewModel() {

    private var entitySet: Boolean = false

    val title: MutableStateFlow<String> = MutableStateFlow("My event")
    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    val startProfileSpinnerPosition: MutableStateFlow<Int> = MutableStateFlow(0)
    val endProfileSpinnerPosition: MutableStateFlow<Int> = MutableStateFlow(0)

    val scheduledDays: MutableStateFlow<Int> = MutableStateFlow(WEEKDAYS)
    val scheduled: MutableStateFlow<Boolean> = MutableStateFlow(true)

    val startTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())
    val endTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now().plusMinutes(30))

    val startAndEndDate: Flow<Pair<LocalDateTime, LocalDateTime>?> = combine(scheduledDays, startTime, endTime)
    { days, startLocalTime, endLocalTime ->
        if (days == WeekDay.NONE) {

            val now: LocalDateTime = LocalDateTime.now()

            val start: LocalDateTime = now
                .withHour(startLocalTime.hour)
                .withMinute(startLocalTime.minute)
                .withSecond(0)
                .withNano(0)

            var end: LocalDateTime = now
                .withHour(endLocalTime.hour)
                .withMinute(endLocalTime.minute)
                .withSecond(0)
                .withNano(0)

            if (start >= end) {
                end = end.plusDays(1)
            }

            Pair(start, end)
        } else null
    }

    private var alarmId: Long? = null
    private var isScheduled: Boolean = false

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val eventsFlow: Flow<ViewEvent> = channel.receiveAsFlow()

    sealed class ViewEvent {

        data class ShowDialogEvent(val dialogType: DialogType): ViewEvent()
        data class OnCreateAlarmEvent(val alarm: Alarm): ViewEvent()
        data class OnUpdateAlarmEvent(val alarm: Alarm): ViewEvent()

        object OnCancelChangesEvent: ViewEvent()
    }

    enum class DialogType {
        SCHEDULED_DAYS,
        START_TIME,
        END_TIME
    }

    suspend fun getEnabledAlarms(): List<AlarmRelation> {
        return alarmRepository.getEnabledAlarms() ?: listOf()
    }

    suspend fun addAlarm(alarm: Alarm): Long {
        return withContext(viewModelScope.coroutineContext) {
            alarmRepository.addAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }

    fun onSaveChangesButtonClick() {
        viewModelScope.launch {
            channel.send(if (alarmId != null) {
                ViewEvent.OnUpdateAlarmEvent(getAlarm())
            } else {
                ViewEvent.OnCreateAlarmEvent(getAlarm())
            })
        }
    }

    fun onCancelButtonClick() {
        viewModelScope.launch {
            channel.send(ViewEvent.OnCancelChangesEvent)
        }
    }

    fun onStartTimeTextViewClick() {
        viewModelScope.launch {
            channel.send(ViewEvent.ShowDialogEvent(DialogType.START_TIME))
        }
    }

    fun onEndTimeTextViewClick() {
        viewModelScope.launch {
            channel.send(ViewEvent.ShowDialogEvent(DialogType.END_TIME))
        }
    }

    fun onDaysSelectButtonClick() {
        viewModelScope.launch {
            channel.send(ViewEvent.ShowDialogEvent(DialogType.SCHEDULED_DAYS))
        }
    }

    fun getEndProfile(): Profile {
        return profilesStateFlow.value[endProfileSpinnerPosition.value]
    }

    fun getStartProfile(): Profile {
        return profilesStateFlow.value[startProfileSpinnerPosition.value]
    }

    private fun getPosition(uuid: UUID, profiles: List<Profile>): Int {
        for ((index, i) in profiles.withIndex()) {
            if (i.id == uuid) {
                return index
            }
        }
        return 0
    }

    fun setEntity(alarmRelation: AlarmRelation, profiles: List<Profile>) {
        if (!entitySet) {

            val alarm: Alarm = alarmRelation.alarm
            val startProfile: Profile = alarmRelation.startProfile
            val endProfile: Profile = alarmRelation.endProfile

            title.value = alarm.title
            scheduledDays.value = alarm.scheduledDays
            startTime.value = alarm.startTime
            endTime.value = alarm.endTime
            scheduled.value = alarm.isScheduled
            alarmId = alarm.id
            isScheduled = alarm.isScheduled

            startProfileSpinnerPosition.value = getPosition(startProfile.id, profiles)
            endProfileSpinnerPosition.value = getPosition(endProfile.id, profiles)

            entitySet = true
        }
    }

    private fun getAlarm(): Alarm {
        return Alarm(
            id = if (alarmId != null) alarmId!! else 0,
            title = title.value.ifEmpty { "No title" },
            startProfileUUID = getStartProfile().id,
            endProfileUUID = getEndProfile().id,
            startTime = startTime.value,
            endTime = endTime.value,
            zoneId = ZoneId.systemDefault(),
            isScheduled = scheduled.value,
            scheduledDays = scheduledDays.value,
        )
    }

    override fun onCleared() {
        super.onCleared()
        channel.close()
    }
}