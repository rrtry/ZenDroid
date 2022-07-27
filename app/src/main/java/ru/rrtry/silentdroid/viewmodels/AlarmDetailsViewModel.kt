package ru.rrtry.silentdroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.rrtry.silentdroid.core.ScheduleCalendar
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
import ru.rrtry.silentdroid.db.repositories.ProfileRepository
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.core.WeekDay
import ru.rrtry.silentdroid.core.WeekDay.Companion.ALL_DAYS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AlarmDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val alarmRepository: AlarmRepository
): ViewModel() {

    private var isEntitySet: Boolean = false

    val title: MutableStateFlow<String> = MutableStateFlow("My event")
    val alarms: Flow<List<AlarmRelation>> = alarmRepository.observeAlarms()
    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    val startProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val endProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)

    val scheduledDays: MutableStateFlow<Int> = MutableStateFlow(ALL_DAYS)
    val scheduled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val startTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())
    val endTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now().plusMinutes(30))

    val startAndEndDate: Flow<Pair<LocalDateTime, LocalDateTime>?> = combine(scheduledDays, startTime, endTime)
    { days, startLocalTime, endLocalTime ->
        if (days == WeekDay.NONE) {
            ScheduleCalendar.getStartAndEndDate(
                startLocalTime,
                endLocalTime
            )
        } else null
    }

    private var alarmId: Int? = null
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

    suspend fun addAlarm(alarm: Alarm): Int {
        return withContext(viewModelScope.coroutineContext) {
            alarmRepository.addAlarm(alarm)
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        withContext(viewModelScope.coroutineContext) {
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

    fun setProfiles(profiles: List<Profile>) {
        if (alarmId == null && !isEntitySet) {
            profiles.random().let { profile ->
                startProfile.value = profile
            }
            profiles.random().let { profile ->
                endProfile.value = profile
            }
            isEntitySet = true
        }
    }

    fun setEntity(alarmRelation: AlarmRelation) {
        if (isEntitySet) return
        val alarm: Alarm = alarmRelation.alarm

        startProfile.value = alarmRelation.startProfile
        endProfile.value = alarmRelation.endProfile

        title.value = alarm.title
        scheduledDays.value = alarm.scheduledDays
        startTime.value = alarm.startTime
        endTime.value = alarm.endTime
        scheduled.value = alarm.isScheduled
        alarmId = alarm.id
        isScheduled = alarm.isScheduled

        isEntitySet = true
    }

    private fun getAlarm(): Alarm {
        return Alarm(
            id = if (alarmId != null) alarmId!! else 0,
            title = title.value.trim().ifBlank { "No title" },
            startProfileUUID = startProfile.value!!.id,
            endProfileUUID = endProfile.value!!.id,
            startTime = startTime.value,
            endTime = endTime.value,
            isScheduled = scheduled.value,
            scheduledDays = scheduledDays.value,
            zoneId = ZoneId.systemDefault(),
        )
    }

    override fun onCleared() {
        super.onCleared()
        channel.close()
    }
}