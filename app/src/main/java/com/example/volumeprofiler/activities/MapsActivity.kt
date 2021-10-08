package com.example.volumeprofiler.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.BottomSheetFragment
import com.example.volumeprofiler.fragments.MapsCoordinatesFragment
import com.example.volumeprofiler.models.LocationRelation
import com.example.volumeprofiler.util.GeocoderUtil
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ln

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(),
        MapsCoordinatesFragment.Callback,
        OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        BottomSheetFragment.Callbacks {

    private val viewModel: MapsSharedViewModel by viewModels()

    private var marker: Marker? = null
    private var circle: Circle? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap

    @Inject lateinit var geocoderUtil: GeocoderUtil

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
        if (isNightModeEnabled()) {
            setNightStyle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.google_maps_activity)
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

    private fun setNightStyle(): Unit {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night))
    }

    /*
    private fun requestLocationPermission(): Unit {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
     */

    private fun bounceInterpolatorAnimation(): Unit {
        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration: Long = 1500

        val interpolator = BounceInterpolator()

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
        viewModel.animateMovement = animateCameraMovement
        viewModel.setLatLng(latLng)
    }

    private fun setLocation(latLng: LatLng): Unit {
        if (marker != null) {
            marker!!.remove()
            circle!!.remove()
        }
        addMarker()
        addCircle()
        val zoomLevel: Float = getZoomLevel()
        if (viewModel.animateMovement) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        }
        lifecycleScope.launch {
            val result: String? = geocoderUtil.getAddressFromLatLng(latLng)
            if (result != null) {
                viewModel.setAddress(result)
                marker!!.title = result
            }
        }
        bounceInterpolatorAnimation()
    }

    private fun getZoomLevel(): Float {
        var zoomLevel: Float = 11f
        if (circle != null) {
            val radius = circle!!.radius + circle!!.radius / 2
            val scale = radius / 500
            zoomLevel = (16 - ln(scale) / ln(2.0)).toFloat()
        }
        return zoomLevel
    }

    private fun addCircle(): Unit {
        circle = mMap.addCircle(CircleOptions()
                .center(viewModel.getLatLng()!!)
                .radius(viewModel.getRadius().toDouble())
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.geofence_fill_color))
    }

    private fun addMarker(): Unit {
        marker = mMap.addMarker(MarkerOptions().position(viewModel.getLatLng()!!).title(viewModel.getAddress()))
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getRecentLocation(): Unit {
        fusedLocationProvider.lastLocation.addOnSuccessListener {
            if (it != null && TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli() - it.time) < DWELL_LIMIT) {
                updateCoordinates(LatLng(it.latitude, it.longitude), false)
            } else {
                getCurrentLocation()
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getCurrentLocation(): Unit {
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
                viewModel.setBottomSheetState(newState)
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun isNightModeEnabled(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.latLng.collect {
                        if (it != null) {
                            setLocation(it)
                        } else if (ContextCompat.checkSelfPermission(
                                        this@MapsActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            getRecentLocation()
                         }
                    }
                }
                launch {
                    viewModel.radius.collect {
                        circle?.radius = it.toDouble()
                        if (circle != null && marker != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker!!.position, getZoomLevel()))
                        }
                    }
                }
            }
        }
        mMap.setOnMapClickListener(this)
        mMap.setOnCameraMoveStartedListener(this)
        mMap.setOnMarkerDragListener(this)
        if (isNightModeEnabled()) {
            setNightStyle()
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    override fun onMapClick(p0: LatLng) {
        updateCoordinates(p0, true)
    }

    override fun setState(state: Int) {
        bottomSheetBehavior.state = state
    }

    override fun collapseBottomSheet() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
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
        if (p0 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            hideSoftInput()
            collapseBottomSheet()
        }
    }

    override fun onMarkerDragStart(p0: Marker) {
        hideSoftInput()
        collapseBottomSheet()
    }

    override fun onMarkerDrag(p0: Marker) {
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

    companion object {

        private const val DWELL_LIMIT: Byte = 30
        private const val EXTRA_LOCATION_TRIGGER: String = "location"

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                if (locationRelation != null) this.putExtra(EXTRA_LOCATION_TRIGGER, locationRelation)
            }
        }
    }
}