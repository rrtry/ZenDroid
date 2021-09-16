package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class EditAlarmViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
): ViewModel() {

    var areArgsSet: Boolean = false

    sealed class Event {
        data class ShowDialogEvent(val dialogType: DialogType): Event()
        object ShowPopupWindowEvent: Event()
    }

    enum class DialogType {
        DAYS_SELECTION,
        TIME_SELECTION
    }

    val profilesLiveData: LiveData<List<Profile>> = profileRepository.observeProfiles().asLiveData()

    val selectedProfile: MutableLiveData<Profile> = MutableLiveData()
    val scheduledDays: MutableLiveData<ArrayList<Int>> = MutableLiveData()
    val startTime: MutableLiveData<LocalDateTime> = MutableLiveData()

    private val channel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = channel.receiveAsFlow()

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

    fun setArgs(alarmTrigger: AlarmTrigger): Unit {
        selectedProfile.value = alarmTrigger.profile
        scheduledDays.value = alarmTrigger.alarm.scheduledDays
        startTime.value = alarmTrigger.alarm.localDateTime
    }

    fun getAlarm(): Alarm? {

    }

    override fun onCleared(): Unit {
        super.onCleared()
        channel.close()
    }
}