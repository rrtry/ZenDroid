package com.example.volumeprofiler.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.BottomSheetFragment
import com.example.volumeprofiler.fragments.MapsCoordinatesFragment
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class MapsActivity : AppCompatActivity(), MapsCoordinatesFragment.Callback, OnMapReadyCallback, GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveStartedListener, BottomSheetFragment.Callbacks {

    private val sharedViewModel: MapsSharedViewModel by viewModels()
    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private var circle: Circle? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var fusedLocationProvider: FusedLocationProviderClient

    private var mScaleFactor: Float = 1f
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Log.i("MapsActivity", "onScale()")
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = 0.1f.coerceAtLeast(mScaleFactor.coerceAtMost(5.0f))
            circle!!.radius *= mScaleFactor
            return true
        }
    }
    private lateinit var mScaleDetector: ScaleGestureDetector

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("MapsActivity", "ACCESS_FINE_LOCATION granted")
        }
        else {
            Log.i("MapsActivity", "ACCESS_FINE_LOCATION denied")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i("MapsActivity", "onConfigurationChanged")
    }

    private fun validateGesture(it: MotionEvent): Boolean {
        val center: Point = mMap.projection.toScreenLocation(marker!!.position)
        return (it.x - center.x).pow(2) + (it.y - center.y).pow(2) < circle!!.radius.pow(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.google_maps_activity)
        mScaleDetector = ScaleGestureDetector(this, scaleListener)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.containerBottomSheet))
        setBottomSheetBehaviour()
        addBottomSheetFragment()
        getMap()
    }

    private fun getMap(): Unit {
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun requestLocationPermission(): Unit {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun bounceInterpolatorAnimation(): Unit {
        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration: Long = 1500

        val interpolator: BounceInterpolator = BounceInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val remainingTime: Float = (1 - interpolator.getInterpolation(elapsed.toFloat() / duration)).coerceAtLeast(0f)
                marker!!.setAnchor(0.5f, 1.0f + 0.5f * remainingTime)
                if (remainingTime > 0.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun updateCoordinates(latLng: LatLng, animateCameraMovement: Boolean): Unit {
        sharedViewModel.animateCameraMovement = animateCameraMovement
        sharedViewModel.latLng.value = latLng
    }

    private suspend fun getAddressThoroughfare(latLng: LatLng): String? {
        val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
        return withContext(Dispatchers.IO) {
            val addressList: MutableList<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address: Address = addressList[0]
                val addressLine: String = address.getAddressLine(0)
                addressLine
            } else {
                null
            }
        }
    }

    private fun setLocation(latLng: LatLng): Unit {
        if (marker != null) {
            marker!!.remove()
            circle!!.remove()
        }
        addMarker()
        addCircle()
        val animateCameraMovement: Boolean = sharedViewModel.animateCameraMovement
        val currentZoomLevel: Float = mMap.cameraPosition.zoom
        val zoomLevel: Float = getZoomLevel()
        if (animateCameraMovement) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        }
        lifecycleScope.launch {
            val result: String? = getAddressThoroughfare(latLng)
            sharedViewModel.addressLine.value = result!!
            marker!!.title = result
        }
        marker!!.isDraggable = true
        bounceInterpolatorAnimation()
    }

    private fun getZoomLevel(): Float {
        var zoomLevel: Float = 11f
        if (circle != null) {
            val radius = circle!!.radius + circle!!.radius / 2
            val scale = radius / 500
            zoomLevel = (16 - Math.log(scale) / Math.log(2.0)).toFloat()
        }
        return zoomLevel
    }

    private fun addCircle(): Unit {
        circle = mMap.addCircle(CircleOptions()
                .center(sharedViewModel.latLng.value)
                .radius(10000.0)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.geofence_fill_color))
    }

    private fun addMarker(): Unit {
        marker = mMap.addMarker(MarkerOptions().position(sharedViewModel.latLng.value).title(sharedViewModel.addressLine.value))
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun obtainLocation(): Unit {
        fusedLocationProvider.lastLocation.addOnSuccessListener {
            if (it != null && TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli() - it.time) < DWELL_LIMIT) {
                Log.i("MapsActivity", "using cached location")
                updateCoordinates(LatLng(it.latitude, it.longitude), false)
            } else {
                obtainCurrentLocation()
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun obtainCurrentLocation(): Unit {
        fusedLocationProvider.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
            override fun isCancellationRequested(): Boolean {
                return true
            }

            override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                return this
            }

        }).addOnSuccessListener {
            if (it != null) {
                updateCoordinates(LatLng(it.latitude, it.longitude), false)
            } else {
                Log.i("MapsActivity", "current location is null")
            }
        }
    }

    private fun addBottomSheetFragment(): Unit {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(R.id.containerBottomSheet)
        if (fragment == null) {
            val bottomSheetFragment: BottomSheetFragment = BottomSheetFragment()
            supportFragmentManager.beginTransaction()
                    .add(R.id.containerBottomSheet, bottomSheetFragment)
                    .commit()
        }
    }

    private fun setBottomSheetBehaviour(): Unit {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.containerBottomSheet))
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                sharedViewModel.bottomSheetState.value = newState
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        bottomSheetBehavior.state = sharedViewModel.bottomSheetState.value!!
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        sharedViewModel.latLng.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                Log.i("MapsActivity", "observing data")
                setLocation(it)
            }
        })
        mMap.setOnMapClickListener(this)
        mMap.setOnCameraMoveStartedListener(this)
        mMap.setOnMarkerDragListener(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (sharedViewModel.latLng.value == null) {
                obtainLocation()
            }
        } else {
            requestLocationPermission()
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        private const val DWELL_LIMIT: Byte = 60
    }

    override fun onMapClick(p0: LatLng) {
        updateCoordinates(p0, true)
    }

    override fun setState(state: Int) {
        bottomSheetBehavior.state = state
    }

    override fun collapseBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun hideSoftInput(): Unit {
        val inputManager: InputMethodManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
    }

    override fun setPeekHeight(height: Int) {
        bottomSheetBehavior.peekHeight = height
    }

    override fun getBottomSheetState(): Int {
        return bottomSheetBehavior.state
    }

    override fun setGesturesEnabled(enabled: Boolean) {
        mMap.uiSettings.setAllGesturesEnabled(enabled)
    }

    override fun onCameraMoveStarted(p0: Int) {
        hideSoftInput()
        collapseBottomSheet()
    }

    override fun onMarkerDragStart(p0: Marker) {
        hideSoftInput()
        collapseBottomSheet()
    }

    override fun onMarkerDrag(p0: Marker) {
        Log.i("MapsActivity", "onMarkerDrag(), ${p0.position}")
    }

    override fun onMarkerDragEnd(p0: Marker) {
        updateCoordinates(LatLng(p0.position.latitude, p0.position.longitude), true)
    }

    override fun setHalfExpandedRatio(ratio: Float) {
        bottomSheetBehavior.halfExpandedRatio = ratio
    }

    override fun getPeekHeight(): Int {
        return bottomSheetBehavior.peekHeight
    }
}