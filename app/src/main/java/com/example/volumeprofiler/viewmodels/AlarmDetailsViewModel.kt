package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.util.AlarmUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AlarmDetailsViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
): ViewModel() {

    private var areArgsSet: Boolean = false

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    val selectedSpinnerPosition: MutableStateFlow<Int> = MutableStateFlow(-1)
    val scheduledDays: MutableStateFlow<ArrayList<Int>> = MutableStateFlow(arrayListOf())
    val localTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())
    val weekDaysLocalTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())

    val scheduleUIUpdates: Flow<Boolean> = combine(localTime, scheduledDays) {
        localTime, scheduledDays -> scheduledDays.isEmpty() && localTime > LocalTime.now()
    }
    val readCalendarPermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var id: Long? = null
    private var isScheduled: Boolean = false

    private val channel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = channel.receiveAsFlow()

    sealed class Event {

        data class ShowDialogEvent(val dialogType: DialogType): Event()

        object QueryAvailableCalendarsEvent : Event()
        object RequestReadCalendarPermission: Event()
    }

    enum class DialogType {
        DAYS_SELECTION,
        TIME_SELECTION
    }

    fun onTimeSelectButtonClick(): Unit {
        viewModelScope.launch {
            channel.send(Event.ShowDialogEvent(DialogType.TIME_SELECTION))
        }
    }

    fun onDaysSelectButtonClick(): Unit {
        viewModelScope.launch {
            channel.send(Event.ShowDialogEvent(DialogType.DAYS_SELECTION))
        }
    }

    fun onQueryCalendarButtonClick(): Unit {
        viewModelScope.launch {
            if (readCalendarPermissionGranted.value) {
                channel.send(Event.QueryAvailableCalendarsEvent)
            } else {
                channel.send(Event.RequestReadCalendarPermission)
            }
        }
    }

    private fun getNextInstanceDate(): Instant {
        return AlarmUtil.getNextAlarmTime(scheduledDays.value, localTime.value)
            .toInstant()
    }

    private fun getAlarmState(): Int {
        return if (isScheduled) 1 else 0
    }

    private fun getProfileUUID(): UUID {
        return profilesStateFlow.value[selectedSpinnerPosition.value].id
    }

    fun getProfile(): Profile {
        return profilesStateFlow.value[selectedSpinnerPosition.value]
    }

    fun getAlarmId(): Long? {
        return this.id
    }

    private fun getPosition(uuid: UUID, profiles: List<Profile>): Int {
        for ((index, i) in profiles.withIndex()) {
            if (i.id == uuid) {
                return index
            }
        }
        return 0
    }

    fun setArgs(alarmRelation: AlarmRelation?, profiles: List<Profile>): Unit {
        if (!areArgsSet && alarmRelation != null) {
            selectedSpinnerPosition.value = getPosition(alarmRelation.profile.id, profiles)
            scheduledDays.value = alarmRelation.alarm.scheduledDays
            localTime.value = alarmRelation.alarm.localStartTime

            id = alarmRelation.alarm.id
            isScheduled = alarmRelation.alarm.isScheduled == 1

            areArgsSet = true
        }
    }

    fun getAlarm(): Alarm {
        val alarm: Alarm = Alarm(
            profileUUID = getProfileUUID(),
            localStartTime = localTime.value,
            instanceStartTime = getNextInstanceDate(),
            isScheduled = getAlarmState(),
            scheduledDays = scheduledDays.value,
            zoneId = ZoneId.systemDefault(),
        )
        if (id != null) {
            alarm.id = this.id!!
        }
        return alarm
    }

    override fun onCleared(): Unit {
        super.onCleared()
        channel.close()
    }
}