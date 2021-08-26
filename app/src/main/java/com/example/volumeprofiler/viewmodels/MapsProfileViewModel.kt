package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile

class MapsProfileViewModel: ViewModel() {

    val repository: Repository = Repository.get()

    val profiles: LiveData<List<Profile>> = repository.observeProfiles().asLiveData()
}