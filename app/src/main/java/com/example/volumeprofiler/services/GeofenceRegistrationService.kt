package com.example.volumeprofiler.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest.permission.*
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.util.GeofenceUtil
import com.example.volumeprofiler.util.checkSelfPermission
import com.example.volumeprofiler.util.createGeofenceRegistrationNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceRegistrationService: Service() {

    private val job: Job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var repository: LocationRepository

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private suspend fun registerGeofences(): Unit {
        val geofences: List<LocationRelation> = repository.getLocations()
        if (geofences.isNotEmpty()) {
            for (i in geofences) {
                if (i.location.enabled == 1.toByte()) {
                    geofenceUtil.addGeofence(
                        i.location,
                        i.onEnterProfile,
                        i.onExitProfile
                    )
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)

        startForeground(SERVICE_ID, createGeofenceRegistrationNotification(this))

        if (checkSelfPermission(this, ACCESS_FINE_LOCATION)) {
            scope.launch {
                val request = launch {
                    registerGeofences()
                }
                request.join()
                stopService()
            }
        } else {
            stopService()
        }
        return START_STICKY
    }

    private fun stopService(): Unit {
        stopForeground(true)
        stopSelf(SERVICE_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private const val SERVICE_ID: Int = 164
    }
}