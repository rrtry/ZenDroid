package com.example.volumeprofiler.activities

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.MatrixCursor
import android.graphics.Color
import android.location.Address
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.BaseColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
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
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
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
    private lateinit var searchView: SearchView

    private var sheetState: Int = STATE_COLLAPSED

    @Inject
    lateinit var geocoderUtil: GeocoderUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.google_maps_activity)
        searchView = findViewById(R.id.searchView)
        setSearchableInfo()
        setSearchViewSuggestions()
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        setBottomSheetBehaviour(savedInstanceState)
        addBottomSheetFragment()
        getMap()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SHEET_STATE, sheetState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSearchIntent(intent)
    }

    private fun handleSearchIntent(intent: Intent?): Unit {
        if (Intent.ACTION_SEARCH == intent?.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                searchView.setQuery(query, false)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isNightModeEnabled()) {
            setNightStyle()
        }
    }

    private fun setSearchableInfo(): Unit {
        val searchManager: SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }

    private fun populateCursor(results: List<Address>): Unit {
        val columns: Array<String> = arrayOf(
                BaseColumns._ID,
                COLUMN_LATITUDE,
                COLUMN_LONGITUDE,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
        )
        val matrixCursor: MatrixCursor = MatrixCursor(columns)
        for (i in results.indices) {
            val row: Array<String> = arrayOf(i.toString(),
                    results[i].latitude.toString(),
                    results[i].longitude.toString(),
                    results[i].getAddressLine(0))
            matrixCursor.addRow(row)
        }
        searchView.suggestionsAdapter.changeCursor(matrixCursor)
    }

    private fun executeSearch(query: String): Unit {
        lifecycleScope.launch {
            val results: List<Address>? = geocoderUtil.getAddressFromLocationName(query)
            if (results != null && results.isNotEmpty()) {
                populateCursor(results)
            }
        }
    }

    private fun setSuggestionsAdapter(): SimpleCursorAdapter {
        val adapter = SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                null,
                arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1),
                intArrayOf(android.R.id.text1),
                0)
        searchView.suggestionsAdapter = adapter
        return adapter
    }

    private fun setSearchViewSuggestions(): Unit {
        setSuggestionsAdapter()
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {

            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.cursor
                viewModel.latLng.value = LatLng(
                        cursor.getString(cursor.getColumnIndex(COLUMN_LATITUDE)).toDouble(),
                        cursor.getString(cursor.getColumnIndex(COLUMN_LONGITUDE)).toDouble())
                searchView.setQuery(null, false)
                searchView.setQuery(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)), false)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.length > MIN_LENGTH) {
                    executeSearch(newText)
                }
                return true
            }
        })
    }

    private fun getMap(): Unit {
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setNightStyle(): Unit {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night))
    }

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
        viewModel.setLatLng(latLng)
    }

    private fun setLocation(latLng: LatLng): Unit {
        addMarker()
        addCircle()
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, getZoomLevel()))
        setAddress(latLng)
        bounceInterpolatorAnimation()
    }

    private fun setAddress(latLng: LatLng): Unit {
        lifecycleScope.launch {
            val result: String? = geocoderUtil.getAddressFromLatLng(latLng)
            if (result != null) {
                viewModel.setAddress(result)
                marker!!.title = result
            }
        }
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
        if (circle != null) {
            circle!!.remove()
        }
        circle = mMap.addCircle(CircleOptions()
                .center(viewModel.getLatLng()!!)
                .radius(viewModel.getRadius().toDouble())
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.geofence_fill_color))
    }

    private fun addMarker(): Unit {
        if (marker != null) {
            marker!!.remove()
        }
        marker = mMap.addMarker(MarkerOptions().position(viewModel.getLatLng()!!).title(viewModel.getAddress()))
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getRecentLocation(): Unit {
        fusedLocationProvider.lastLocation.addOnSuccessListener {
            if (it != null && TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli() - it.time) < DWELL_LIMIT_MINUTES) {
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

    private fun setBottomSheetBehaviour(savedInstanceState: Bundle?): Unit {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.containerBottomSheet))
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_HIDDEN) {
                    bottomSheetBehavior.state = STATE_COLLAPSED
                }
                when (newState) {
                    STATE_EXPANDED -> {
                        if (searchView.visibility == View.VISIBLE) {
                            setSearchViewVisibility(View.INVISIBLE, true)
                        }
                    }
                    STATE_COLLAPSED, STATE_HALF_EXPANDED -> {
                        if (searchView.visibility == View.INVISIBLE) {
                            setSearchViewVisibility(View.VISIBLE, true)
                        }
                    }
                }
                sheetState = newState
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })
        if (savedInstanceState != null) {
            sheetState = savedInstanceState.getInt(EXTRA_SHEET_STATE)
            if (sheetState == STATE_EXPANDED) {
                setSearchViewVisibility(View.INVISIBLE, false)
            }
        }
    }

    private fun getTransition(): TransitionSet {
        val transition: TransitionSet = TransitionSet()
        transition.addTarget(R.id.searchView).addTransition(Slide(Gravity.TOP))
        return transition
    }

    private fun setSearchViewVisibility(visibility: Int, animate: Boolean) {
        if (animate) {
            TransitionManager.beginDelayedTransition(findViewById(R.id.rootLayout), getTransition())
        }
        searchView.visibility = visibility
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
        if (bottomSheetBehavior.state != STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_COLLAPSED
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
        if (bottomSheetBehavior.state != STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_COLLAPSED
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

        private const val DWELL_LIMIT_MINUTES: Byte = 15
        private const val EXTRA_LOCATION_TRIGGER: String = "location"
        private const val EXTRA_SHEET_STATE: String = "extra_sheet_state"
        private const val COLUMN_LATITUDE: String = "latitude"
        private const val COLUMN_LONGITUDE: String = "longitude"
        private const val MIN_LENGTH: Int = 10

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                if (locationRelation != null) this.putExtra(EXTRA_LOCATION_TRIGGER, locationRelation)
            }
        }
    }
}