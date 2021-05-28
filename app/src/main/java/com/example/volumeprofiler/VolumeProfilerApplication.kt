package com.example.volumeprofiler

import android.app.Application
import com.example.volumeprofiler.database.Repository

class VolumeProfilerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
    }
}