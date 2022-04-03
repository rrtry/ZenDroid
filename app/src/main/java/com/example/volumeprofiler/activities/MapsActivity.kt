package com.example.volumeprofiler.activities

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.SearchManager
import android.content.*
import android.annotation.SuppressLint
import android.content.Intent.ACTION_SEARCH
import android.content.res.Configuration
import android.database.MatrixCursor
import android.graphics.Color
import android.location.Address
import android.os.*
import android.provider.BaseColumns
import android.util.Log
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.GoogleMapsActivityBinding
import com.example.volumeprofiler.fragments.BottomSheetFragment
import com.example.volumeprofiler.fragments.MapsCoordinatesFragment
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import dagger.hilt.android.AndroidEntryPoint
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
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        BottomSheetFragment.Callbacks {

    private val viewModel: MapsSharedViewModel by viewModels()

    private var marker: Marker? = null
    private var circle: Circle? = null

    private var bindingImpl: GoogleMapsActivityBinding? = null
    private val binding: GoogleMapsActivityBinding get() = bindingImpl!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<out String>>
    private lateinit var profilesQuery: List<Profile>
    private lateinit var taskCancellationSource: CancellationTokenSource

    @Inject
    lateinit var geocoderUtil: GeocoderUtil

    @Inject
    lateinit var geofenceManager: GeofenceManager

    @Inject
    lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.google_maps_activity)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        binding.addGeofenceButton.setOnClickListener {

        }
        binding.recentLocationButton.setOnClickListener {

        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }
        setSearchConfiguration()
        setBottomSheetBehavior()
        addBottomSheetFragment()
        getMap()
    }

    override fun onStart() {
        super.onStart()
        taskCancellationSource = CancellationTokenSource()
    }

    private fun setSearchConfiguration(): Unit {
        setSearchableInfo()
        setSearchViewSuggestions()
    }

    override fun onStop() {
        super.onStop()
        taskCancellationSource.cancel()
    }

    private fun setArgs(): Unit {
        intent.getParcelableExtra<LocationRelation>(EXTRA_LOCATION_RELATION)?.also {
            viewModel.setLocation(it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SEARCH) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                binding.searchView.setQuery(query, false)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (ViewUtil.uiModeNightEnabled(this)) {
            setNightStyle()
        }
    }

    private fun setSearchableInfo(): Unit {
        val searchManager: SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }

    private fun setSuggestionsAdapter(): SimpleCursorAdapter {
        val adapter = SimpleCursorAdapter(
            this,
            R.layout.location_suggestion_item_view,
            null,
            arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1),
            intArrayOf(R.id.event_display_name),
            0)
        binding.searchView.suggestionsAdapter = adapter
        return adapter
    }

    private fun changeCursor(results: List<Address>): Unit {
        val columns: Array<String> = arrayOf(
                BaseColumns._ID,
                COLUMN_LATITUDE,
                COLUMN_LONGITUDE,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
        )
        val matrixCursor: MatrixCursor = MatrixCursor(columns)
        for (i in results.indices) {
            val row: Array<String> = arrayOf(
                i.toString(),
                results[i].latitude.toString(),
                results[i].longitude.toString(),
                results[i].getAddressLine(0)
            )
            matrixCursor.addRow(row)
        }
        binding.searchView.suggestionsAdapter.changeCursor(matrixCursor)
    }

    private fun getAddressFromLocationName(query: String): Unit {
        lifecycleScope.launch {
            val results: List<Address>? = geocoderUtil.getAddressFromLocationName(query)
            if (!results.isNullOrEmpty()) {
                changeCursor(results)
            }
        }
    }

    private fun setSearchViewSuggestions(): Unit {
        setSuggestionsAdapter()
        binding.searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {

            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = binding.searchView.suggestionsAdapter.cursor
                viewModel.latLng.value = LatLng(
                        cursor.getString(cursor.getColumnIndex(COLUMN_LATITUDE)).toDouble(),
                        cursor.getString(cursor.getColumnIndex(COLUMN_LONGITUDE)).toDouble())
                binding.searchView.setQuery(null, false)
                binding.searchView.setQuery(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)), false)
                return true
            }
        })
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length > MIN_LENGTH) {
                    getAddressFromLocationName(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
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

    private fun startMarkerAnimation(): Unit {

        val handler: Handler = Handler(Looper.getMainLooper())
        val start: Long = SystemClock.uptimeMillis()
        val duration: Long = 1500

        val interpolator: BounceInterpolator = BounceInterpolator()

        handler.post(object : Runnable {

            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val remainingTime: Float = (1 - interpolator.getInterpolation(elapsed.toFloat() / duration)).coerceAtLeast(0f)
                marker?.setAnchor(0.5f, 1.0f + 0.5f * remainingTime)
                if (remainingTime > 0.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun updateCoordinates(latLng: LatLng, animateCameraMovement: Boolean): Unit {
        viewModel.setLatLng(latLng)
    }

    private fun targetWithinCameraBounds(latLng: LatLng): Boolean {
        return mMap.projection.visibleRegion.latLngBounds.contains(latLng)
    }

    private fun setLocation(latLng: LatLng): Unit {
        addMarker()
        addCircle()
        val zoomLevel: Float = getZoomFactor()
        if (targetWithinCameraBounds(latLng)) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        }
        setAddress(latLng)
        startMarkerAnimation()
    }

    private fun setAddress(latLng: LatLng): Unit {
        lifecycleScope.launch {
            val result: Address? = geocoderUtil.getAddressFromLatLng(latLng)
            if (result != null) {
                val addressLine: String = result.getAddressLine(0)
                viewModel.setAddress(addressLine)
                marker?.title = addressLine
            }
        }
    }

    private fun getZoomFactor(): Float {
        circle?.let {
            val radius: Double = it.radius + it.radius / 2
            val scale: Double = radius / 400
            return (16 - ln(scale) / ln(2.0)).toFloat()
        }
        return mMap.maxZoomLevel
    }

    private fun addCircle(): Unit {
        circle?.remove()
        circle = mMap.addCircle(CircleOptions()
                .center(viewModel.getLatLng()!!)
                .radius(viewModel.getRadius().toDouble())
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.geofence_fill_color))
    }

    private fun addMarker(): Unit {
        marker?.remove()
        viewModel.getLatLng()?.let {
            marker = mMap.addMarker(MarkerOptions().position(it).title(viewModel.getAddress()))
        }
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getRecentLocation(): Unit {
        fusedLocationProvider.lastLocation
            .addOnFailureListener {
                Log.e("MapsActivity", "Failed to fetch last known location", it)
            }.addOnSuccessListener {
                if (it != null && TimeUnit.MINUTES.toMinutes(Instant.now().toEpochMilli() - it.time) < DWELL_LIMIT_MINUTES) {
                    updateCoordinates(LatLng(it.latitude, it.longitude), false)
                } else {
                    getCurrentLocation()
                }
        }
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getCurrentLocation(): Unit {
        fusedLocationProvider.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, taskCancellationSource.token)
            .addOnCanceledListener {
                Log.w("MapsActivity", "Location request was cancelled")
            }
            .addOnSuccessListener {
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
            supportFragmentManager
                .beginTransaction()
                .add(R.id.containerBottomSheet, BottomSheetFragment())
                .commit()
        }
    }

    private fun setBottomSheetBehavior(): Unit {
        bottomSheetBehavior = from(binding.containerBottomSheet)
        bottomSheetBehavior.isFitToContents = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profilesStateFlow.collect {
                        profilesQuery = it
                        setArgs()
                    }
                }
                launch {
                    viewModel.latLng.collect {
                        if (it != null) {
                            setLocation(it)
                        } else if (checkSelfPermission(this@MapsActivity, ACCESS_FINE_LOCATION)) {
                            getRecentLocation()
                        }
                    }
                }
                launch {
                    viewModel.radius.collect {
                        circle?.radius = it.toDouble()
                        marker?.let {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, getZoomFactor()))
                        }
                    }
                }
            }
        }
        mMap.setOnMapClickListener(this)
        mMap.setOnCameraMoveStartedListener(this)
        mMap.setOnMarkerDragListener(this)
        mMap.setOnMapLongClickListener(this)
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

    override fun onMapLongClick(p0: LatLng) {

    }

    override fun setState(state: Int) {
        bottomSheetBehavior.state = state
    }

    override fun collapseBottomSheet() {
        if (bottomSheetBehavior.state != STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_COLLAPSED
        }
    }

    /*
    private fun takeSnapshot(id: UUID): Unit {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
            viewModel.getLatLng()!!, getZoomFactor()
        )))
        Handler(Looper.getMainLooper()).postDelayed({
            mMap.clear()
            mMap.snapshot {
                if (it != null) {
                    writePreviewBitmap(this, id, it)
                }
                finish()
            }
        }, 500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceUtil.REQUEST_ENABLE_LOCATION_SERVICES) {
            if (resultCode != Activity.RESULT_OK) {
                val snackBar: Snackbar = Snackbar.make(addGeofenceButton, "Enable location services", Snackbar.LENGTH_LONG)
                snackBar.setAction("Enable") {
                    onPermissionRequestResult(true)
                }
                snackBar.show()
            } else {
                onPermissionRequestResult(true)
            }
        }
    } */

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
        when (p0) {
            REASON_GESTURE -> {
                ViewUtil.hideSoftwareInput(this)
                collapseBottomSheet()
            }
            REASON_DEVELOPER_ANIMATION -> {
                Log.i("MapsActivity", "REASON_DEVELOPER_ANIMATION")
            }
        }
    }

    override fun onMarkerDragStart(p0: Marker) {
        ViewUtil.hideSoftwareInput(this)
        collapseBottomSheet()
    }

    override fun onMarkerDrag(p0: Marker) {

    }

    override fun onMarkerDragEnd(p0: Marker) {
        updateCoordinates(LatLng(p0.position.latitude, p0.position.longitude), true)
    }

    override fun setHalfExpandedRatio(ratio: Float) {
        /*
        val previousState: Int = bottomSheetBehavior.state
        bottomSheetBehavior.state = STATE_COLLAPSED
        bottomSheetBehavior.halfExpandedRatio = ratio
        bottomSheetBehavior.state = previousState */
    }

    override fun getPeekHeight(): Int {
        return bottomSheetBehavior.peekHeight
    }

    companion object {

        private const val DWELL_LIMIT_MINUTES: Int = 3 * 60
        private const val EXTRA_LOCATION_RELATION: String = "location"
        private const val EXTRA_SHEET_STATE: String = "extra_sheet_state"
        private const val EXTRA_HALF_EXPANDED_RATIO: String = "extra_half_expanded_ratio"
        private const val COLUMN_LATITUDE: String = "latitude"
        private const val COLUMN_LONGITUDE: String = "longitude"
        private const val MIN_LENGTH: Int = 10

        const val EXTRA_LOCATION: String = "Location"
        const val FLAG_UPDATE_EXISTING: String = "update_existing"

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                if (locationRelation != null) {
                    putExtra(EXTRA_LOCATION_RELATION, locationRelation)
                }
            }
        }
    }
}