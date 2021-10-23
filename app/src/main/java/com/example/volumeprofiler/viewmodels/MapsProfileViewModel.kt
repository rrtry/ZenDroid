package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MapsProfileViewModel @Inject constructor(
        private val repository: ProfileRepository
): ViewModel() {

    val profilesLiveData: LiveData<List<Profile>> = repository.observeProfiles().asLiveData()

    val toRestoreProfilePosition: MutableLiveData<Int> = MutableLiveData(0)
    val toApplyProfilePosition: MutableLiveData<Int> = MutableLiveData(0)

    fun setBindings(): Unit {
        toApplyProfilePosition.value = 0
        toRestoreProfilePosition.value = 0
    }
}