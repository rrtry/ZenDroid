package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class EditAlarmViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
): ViewModel() {

    var areArgsSet: Boolean = false

    val profilesLiveData: LiveData<List<Profile>> = profileRepository.observeProfiles().asLiveData()

    val selectedSpinnerPosition: MutableLiveData<Int> = MutableLiveData(0)
    val scheduledDays: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf())
    val startTime: MutableLiveData<LocalDateTime> = MutableLiveData(LocalDateTime.now())

    private var id: Long? = null
    private var isScheduled: Boolean = false

    private val channel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = channel.receiveAsFlow()

    sealed class Event {

        data class ShowDialogEvent(val dialogType: DialogType): Event()
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

    private fun getProfileUUID(): UUID {
        return profilesLiveData.value!![selectedSpinnerPosition.value!!].id
    }

    fun getProfile(): Profile {
        return profilesLiveData.value!![selectedSpinnerPosition.value!!]
    }

    fun getAlarmId(): Long? {
        return this.id
    }

    private fun getPosition(uuid: UUID): Int {
        for ((index, i) in profilesLiveData.value!!.withIndex()) {
            if (i.id == uuid) {
                return index
            }
        }
        return 0
    }

    fun setArgs(alarmRelation: AlarmRelation?): Unit {
        if (!areArgsSet && alarmRelation != null) {

            selectedSpinnerPosition.value = getPosition(alarmRelation.profile.id)
            scheduledDays.value = alarmRelation.alarm.scheduledDays
            startTime.value = alarmRelation.alarm.localDateTime

            id = alarmRelation.alarm.id
            isScheduled = alarmRelation.alarm.isScheduled == 1

            areArgsSet = true
        }
    }

    fun getAlarm(): Alarm {
        val alarm: Alarm =  Alarm(
            profileUUID = getProfileUUID(),
            localDateTime = startTime.value!!,
            isScheduled = if (isScheduled) 1 else 0,
            scheduledDays = scheduledDays.value!!
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