package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MapsProfileViewModel @Inject constructor(
        private val repository: ProfileRepository
): ViewModel() {

    val profiles: LiveData<List<Profile>> = repository.observeProfiles().asLiveData()
}