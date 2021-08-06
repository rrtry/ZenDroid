package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile

class ViewpagerSharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()
    val profileListLiveData: LiveData<List<Profile>>
        get() {
            return repository.observeProfiles()
        }
}