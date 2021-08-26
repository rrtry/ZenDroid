package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile

class MainActivitySharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()
    val profileListLiveData: LiveData<List<Profile>> = repository.observeProfiles().asLiveData()
}