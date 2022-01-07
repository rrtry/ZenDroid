package com.example.volumeprofiler.activities

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.SearchManager
import android.content.*
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.database.MatrixCursor
import android.graphics.Color
import android.location.Address
import android.location.LocationManager
import android.os.*
import android.provider.BaseColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.BottomSheetFragment
import com.example.volumeprofiler.fragments.MapsCoordinatesFragment
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.entities.Location
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.*
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
    private var pendingAction: Int = 0
    private var floatingItemsVisible: Boolean = true
    private var transitionRunning: Boolean = false

    private lateinit var addGeofenceButton: FloatingActionButton
    private lateinit var recentLocationButton: FloatingActionButton
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var searchView: SearchView
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<out String>>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var profilesQuery: List<Profile>
    private lateinit var taskCancellationSource: CancellationTokenSource

    @Inject
    lateinit var geocoderUtil: GeocoderUtil

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val locationProviderReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                Log.i("MapsActivity", "PROVIDERS_CHANGED_ACTION")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.google_maps_activity)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        coordinatorLayout = findViewById(R.id.rootLayout)
        recentLocationButton = findViewById(R.id.jumpToCurrentLocationButton)
        addGeofenceButton = findViewById(R.id.saveGeofenceButton)

        addGeofenceButton.setOnClickListener {
            pendingAction = ACTION_CREATE_GEOFENCE
            setSuccessfulResult(
                viewModel.getLocation(
                    profilesQuery
                ), hasParcelableData()
            )
        }
        recentLocationButton.setOnClickListener {
            pendingAction = ACTION_DETERMINE_LOCATION
            if (checkSelfPermission(this, ACCESS_FINE_LOCATION)) {
                val weakReference = WeakReference(this)
                geofenceUtil.checkLocationServicesAvailability(this) {
                    weakReference.get()?.getCurrentLocation()
                }
            } else {
                requestLocationPermission()
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionRequestResult(false)
        }
        supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, this,
            { _, result ->
                val permission: String = result.getString(PermissionExplanationDialog.EXTRA_PERMISSION)!!
                if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        requestPermissions()
                    } else {
                        startActivity(getApplicationSettingsIntent(this))
                    }
                }
            })
        setSearchConfiguration()
        setBottomSheetBehavior()
        addBottomSheetFragment()
        getMap()
    }

    private fun requestLocationPermission(): Unit {
        permissionLauncher.launch(
            arrayOf(ACCESS_FINE_LOCATION)
        )
    }

    private fun requestPermissions(): Unit {
        var permissions: Array<String> = arrayOf(
            ACCESS_FINE_LOCATION
        )
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            permissions += ACCESS_BACKGROUND_LOCATION
        }
        if (profileUtil.requiresPhoneStatePermission(viewModel.getLocationRelation(profilesQuery))) {
            permissions += READ_PHONE_STATE
        }
        permissionLauncher.launch(permissions)
    }

    private fun onPermissionRequestResult(resolve: Boolean): Unit {
        when (pendingAction) {
            ACTION_CREATE_GEOFENCE -> {
                setSuccessfulResult(viewModel.getLocation(profilesQuery), hasParcelableData())
            }
            ACTION_DETERMINE_LOCATION -> {
                when {
                    checkSelfPermission(this, ACCESS_FINE_LOCATION) -> {
                        val weakReference: WeakReference<MapsActivity> = WeakReference(this)
                        geofenceUtil.checkLocationServicesAvailability(this) {
                            weakReference.get()?.getRecentLocation()
                        }
                    }
                    resolve -> {
                        requestLocationPermission()
                    }
                    else -> {
                        ViewUtil.showLocationPermissionExplanation(supportFragmentManager)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        taskCancellationSource = CancellationTokenSource()
    }

    private fun setSearchConfiguration(): Unit {
        searchView = findViewById(R.id.searchView)
        setSearchableInfo()
        setSearchViewSuggestions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(locationProviderReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(locationProviderReceiver)
        taskCancellationSource.cancel()
    }

    private fun setArgs(): Unit {
        val locationRelation: LocationRelation? = intent.getParcelableExtra(EXTRA_LOCATION_RELATION)
        if (locationRelation != null) {
            viewModel.setLocation(locationRelation)
        }
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
        if (ViewUtil.uiModeNightEnabled(this)) {
            setNightStyle()
        }
    }

    private fun setSearchableInfo(): Unit {
        val searchManager: SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }

    private fun setSuggestionsAdapter(): SimpleCursorAdapter {
        val adapter = SimpleCursorAdapter(
            this,
            R.layout.location_suggestion_item_view,
            null,
            arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1),
            intArrayOf(R.id.event_display_name),
            0)
        searchView.suggestionsAdapter = adapter
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
        searchView.suggestionsAdapter.changeCursor(matrixCursor)
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
        bounceInterpolatorAnimation()
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
        var zoomFactor: Float = mMap.maxZoomLevel
        if (circle != null) {
            val radius = circle!!.radius + circle!!.radius / 2
            val scale = radius / 400
            zoomFactor = (16 - ln(scale) / ln(2.0)).toFloat()
        }
        return zoomFactor
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
        marker = mMap.addMarker(MarkerOptions().position(viewModel.getLatLng()!!).title(viewModel.getAddress()))
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getRecentLocation(): Unit {
        fusedLocationProvider.lastLocation
            .addOnFailureListener {
                Log.e("MapsActivity", "Failed to fetch last known location", it)
            }
            .addOnSuccessListener {
            if (it != null && TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli() - it.time) < DWELL_LIMIT_MINUTES) {
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
            val bottomSheetFragment: BottomSheetFragment = BottomSheetFragment()
            supportFragmentManager.beginTransaction()
                    .add(R.id.containerBottomSheet, bottomSheetFragment)
                    .commit()
        }
    }

    @SuppressLint("WrongConstant")
    private fun setBottomSheetBehavior(): Unit {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.containerBottomSheet))
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_HIDDEN) {
                    bottomSheetBehavior.state = STATE_COLLAPSED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (floatingItemsVisible) {
                    if (slideOffset > 0.3f) {
                        setSearchViewVisibility(View.INVISIBLE, true)
                        recentLocationButton.hide()
                        addGeofenceButton.hide()
                    } else {
                        setSearchViewVisibility(View.VISIBLE, true)
                        recentLocationButton.show()
                        addGeofenceButton.show()
                    }
                }
            }
        })
    }

    private fun setSearchViewVisibility(visibility: Int, animate: Boolean) {
        if (floatingItemsVisible && !transitionRunning) {
            if (animate) {
                val transition: TransitionSet = TransitionSet()
                transition.addListener(object : TransitionListenerAdapter() {

                    override fun onTransitionStart(transition: Transition) {
                        super.onTransitionStart(transition)
                        transitionRunning = true
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        super.onTransitionEnd(transition)
                        transitionRunning = false
                    }

                })
                transition.addTarget(R.id.searchView).addTransition(Slide(Gravity.TOP))
                TransitionManager.beginDelayedTransition(coordinatorLayout, transition)
            }
            searchView.visibility = visibility
        }
    }

    private fun setFloatingItemsVisibility(visibility: Int): Unit {
        floatingItemsVisible = visibility == View.VISIBLE

        val transition: TransitionSet = TransitionSet()
        transition.addTarget(R.id.searchView).addTransition(Slide(Gravity.TOP))
        TransitionManager.beginDelayedTransition(coordinatorLayout, transition)

        searchView.visibility = visibility

        if (visibility == View.VISIBLE) {
            addGeofenceButton.show()
            recentLocationButton.show()
        } else {
            addGeofenceButton.hide()
            recentLocationButton.hide()
        }
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
                        if (circle != null && marker != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker!!.position, getZoomFactor()))
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
        setFloatingItemsVisibility(if (floatingItemsVisible) View.INVISIBLE else View.VISIBLE)
    }

    override fun setState(state: Int) {
        bottomSheetBehavior.state = state
    }

    override fun collapseBottomSheet() {
        if (bottomSheetBehavior.state != STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_COLLAPSED
        }
    }

    private fun hasParcelableData(): Boolean {
        return intent.extras?.getParcelable<LocationRelation>(EXTRA_LOCATION_RELATION) != null
    }

    @Suppress("MissingPermission")
    private fun setSuccessfulResult(geofence: Location, updateExisting: Boolean): Unit {
        if (geofence.enabled == 1.toByte()) {
            val locationRelation: LocationRelation = viewModel.getLocationRelation(profilesQuery)
            when {
                    !profileUtil.grantedRequiredPermissions(locationRelation) && !geofenceUtil.locationAccessGranted() -> {
                        Snackbar.make(coordinatorLayout, "Missing required permissions", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Grant") {
                                requestPermissions()
                            }
                            .show()
                    }
                    !geofenceUtil.locationAccessGranted() -> {
                        ViewUtil.showLocationPermissionExplanation(supportFragmentManager)
                    }
                    profileUtil.shouldRequestPhonePermission(locationRelation) -> {
                        ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                    }
                    !profileUtil.grantedSystemPreferencesAccess() -> {
                        sendSystemPreferencesAccessNotification(this, profileUtil)
                    }
                    else -> {
                        applyChanges(geofence, updateExisting)
                    }
                }
        } else {
            applyChanges(geofence, updateExisting)
        }
    }

    private fun applyChanges(geofence: Location, updateExisting: Boolean): Unit {
        val intent: Intent = Intent().apply {
            putExtra(EXTRA_LOCATION, geofence)
            putExtra(FLAG_UPDATE_EXISTING, updateExisting)
        }
        setResult(RESULT_OK, intent)
        takeMapSnapshotAndFinish(geofence.previewImageId)
    }

    private fun takeMapSnapshotAndFinish(id: UUID): Unit {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
            viewModel.getLatLng()!!, getZoomFactor()
        )))
        mMap.clear()
        mMap.snapshot {
            if (it != null) {
                writeCompressedBitmap(this, id, it)
            }
            finish()
        }
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
        val previousState: Int = bottomSheetBehavior.state
        bottomSheetBehavior.state = STATE_COLLAPSED
        bottomSheetBehavior.halfExpandedRatio = ratio
        bottomSheetBehavior.state = previousState
    }

    override fun getPeekHeight(): Int {
        return bottomSheetBehavior.peekHeight
    }

    companion object {

        private const val DWELL_LIMIT_MINUTES: Int = 3 * 60
        private const val EXTRA_POST_PERMISSION_CHECK_ACTION: String = "extra_post_permission_check_action"
        private const val ACTION_CREATE_GEOFENCE: Int = 182
        private const val ACTION_DETERMINE_LOCATION: Int = 192
        private const val EXTRA_LOCATION_RELATION: String = "location"
        private const val EXTRA_SHEET_STATE: String = "extra_sheet_state"
        private const val EXTRA_HALF_EXPANDED_RATIO: String = "extra_half_expanded_ratio"
        private const val COLUMN_LATITUDE: String = "latitude"
        private const val COLUMN_LONGITUDE: String = "longitude"
        private const val MIN_LENGTH: Int = 10
        private const val REQUEST_TURN_DEVICE_LOCATION_ON: Int = 4

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