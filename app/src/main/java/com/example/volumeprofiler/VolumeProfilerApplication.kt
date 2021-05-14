package com.example.volumeprofiler

import android.app.Application

class VolumeProfilerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
    }
}