package com.example.volumeprofiler.ui.activities

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.SearchManager.SUGGEST_COLUMN_TEXT_1
import android.content.*
import android.content.Intent.ACTION_SEARCH
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.*
import android.provider.BaseColumns._ID
import android.util.Log
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.SuggestionsAdapter
import com.example.volumeprofiler.core.FileManager
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.databinding.GoogleMapsActivityBinding
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.LocationSuggestion
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.ui.fragments.BottomSheetFragment
import com.example.volumeprofiler.ui.fragments.MapThemeSelectionDialog
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_ICON
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LATITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LONGITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_VIEW_TYPE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_LOCATION_RECENT_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_LOCATION_SUGGESTION
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ID_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.SUGGESTION_TEXT_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_LOCATION_RECENT_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_LOCATION_SUGGESTION
import com.example.volumeprofiler.util.ViewUtil.Companion.convertDipToPx
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.ui.FloatingActionMenuController
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel.ViewEvent.*
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Runnable
import java.lang.ref.WeakReference
import kotlin.collections.HashSet

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        DetailsViewContract<Location>,
        GeofenceManager.LocationRequestListener,
        NetworkStateObserver.NetworkCallback,
        SuggestionsAdapter.Callback,
        GoogleMap.OnInfoWindowLongClickListener,
        FloatingActionMenuController.MenuStateListener {

    interface ItemSelectedListener {

        fun onItemSelected(itemId: Int)
    }

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var fileManager: FileManager

    private val viewModel: GeofenceSharedViewModel by viewModels()

    private var mMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null

    private var bindingImpl: GoogleMapsActivityBinding? = null
    private val binding: GoogleMapsActivityBinding get() = bindingImpl!!

    private var isSuggestionClicked: Boolean = false
    private var floatingMenuVisible: Boolean = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var profiles: List<Profile>
    private lateinit var taskCancellationSource: CancellationTokenSource
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private lateinit var floatingActionMenuController: FloatingActionMenuController
    private lateinit var networkObserver: NetworkStateObserver

    override fun onSuggestionClick(suggestion: LocationSuggestion, itemViewType: Int) {

        isSuggestionClicked = true

        if (itemViewType == VIEW_TYPE_LOCATION_SUGGESTION) {
            viewModel.addSuggestion(suggestion)
        }
        viewModel.latLng.value = Pair(LatLng(suggestion.latitude, suggestion.longitude), false)
        viewModel.address.value = suggestion.address

        binding.searchView.setQuery(suggestion.address, false)
        binding.searchView.clearFocus()

        isSuggestionClicked = false
    }

    override fun onRemoveRecentQuery(locationSuggestion: LocationSuggestion) {
        viewModel.removeSuggestion(locationSuggestion).invokeOnCompletion {
            setLocationSuggestions(binding.searchView.query.toString())
        }
    }

    override fun onNetworkAvailable() {
        runOnUiThread {
            showSnackbar(
                findViewById(android.R.id.content),
                "Network is available",
                LENGTH_LONG
            )
        }
    }

    override fun onNetworkLost() {
        runOnUiThread {
            showSnackbar(
                findViewById(android.R.id.content),
                "Network is unavailable",
                LENGTH_LONG
            )
        }
    }

    override fun onUpdate(location: Location) {
        lifecycleScope.launch {
            viewModel.updateLocation(location)
            captureSnapshot(location)
        }
    }

    override fun onInsert(location: Location) {
        lifecycleScope.launch {
            viewModel.addLocation(location)
            captureSnapshot(location)
        }
    }

    override fun onFinish(result: Boolean) {
        finish()
    }

    private fun onSnapshotReady(bitmap: Bitmap?, uuid: UUID) {
        lifecycleScope.launch {
            fileManager.writeThumbnail(uuid, bitmap)
        }.invokeOnCompletion {
            finish()
        }
    }

    private fun captureSnapshot(location: Location) {
        updateCameraBounds(LatLng(location.latitude, location.longitude), location.radius, false)
        Handler(Looper.getMainLooper()).postDelayed({
            mMap?.clear()
            mMap?.snapshot {
                onSnapshotReady(it, location.previewImageId)
            }
        }, 100)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_FLOATING_ACTION_MENU_VISIBLE, floatingMenuVisible)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
           floatingMenuVisible = savedInstanceState.getBoolean(EXTRA_FLOATING_ACTION_MENU_VISIBLE, false)
        }

        bindingImpl = GoogleMapsActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        networkObserver = NetworkStateObserver(WeakReference(this))
        geocoder = Geocoder(this, Locale.getDefault())
        floatingActionMenuController = FloatingActionMenuController(
            WeakReference(this),
            floatingMenuVisible
        )

        registerForPermissionRequestResult()
        setSearchConfiguration()
        setBottomSheetBehavior()
        setBottomNavigationListeners()
        addBottomSheetFragment()
        getMap()

        lifecycle.addObserver(networkObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(networkObserver)
    }

    private fun registerForPermissionRequestResult() {
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) geofenceManager.checkLocationServicesAvailability(this)
        }
    }

    private fun onLocationRequest() {
        if (checkPermission(ACCESS_FINE_LOCATION)) {
            geofenceManager.checkLocationServicesAvailability(this@MapsActivity)
        } else {
            locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
        toggleFloatingActionMenuVisibility()
    }

    private fun showMapStylesDialog() {
        toggleFloatingActionMenuVisibility()
        MapThemeSelectionDialog().show(supportFragmentManager, null)
    }

    private fun toggleFloatingActionMenuVisibility() {
        floatingActionMenuController.toggle(
            binding.expandableFab,
            binding.overlay,
            binding.saveGeofenceButton,
            binding.saveChangesLabel,
            binding.overlayOptionsFab,
            binding.mapOverlayLabel,
            binding.currentLocationFab,
            binding.currentLocationLabel
        )
    }

    private fun setSearchableInfo() {
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        binding.searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }

    private fun setSearchConfiguration() {
        setSearchableInfo()
        setSearchViewSuggestions()
    }

    private fun addBottomSheetFragment() {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(R.id.containerBottomSheet)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.containerBottomSheet, BottomSheetFragment())
                .commit()
        }
    }

    private fun setBottomNavigationListeners() {
        binding.bottomNavigation.doOnLayout {
            bottomSheetBehavior.peekHeight = binding.bottomNavigation.height + convertDipToPx(BOTTOM_SHEET_OFFSET)
        }
        binding.bottomNavigation.setOnItemReselectedListener {
            bottomSheetBehavior.state = STATE_EXPANDED
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            supportFragmentManager.findFragmentById(R.id.containerBottomSheet)?.let {
                (it as ItemSelectedListener).onItemSelected(item.itemId)
            }
            bottomSheetBehavior.state = STATE_EXPANDED
            true
        }
    }

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior = from(binding.containerBottomSheet)
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.halfExpandedRatio = 0.5f
        binding.searchView.doOnPreDraw { view -> bottomSheetBehavior.expandedOffset = view.bottom }
    }

    override fun onStart() {
        super.onStart()
        taskCancellationSource = CancellationTokenSource()
    }

    override fun onStop() {
        super.onStop()
        taskCancellationSource.cancel()
    }

    private fun setEntity() {
        intent.getParcelableExtra<LocationRelation>(EXTRA_LOCATION_RELATION)?.also {
            viewModel.setEntity(it)
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

    private fun setConnectionErrorCursor() {
        MatrixCursor(arrayOf(_ID, COLUMN_VIEW_TYPE, COLUMN_ICON, SUGGEST_COLUMN_TEXT_1)).apply {
            addRow(arrayOf(
                ID_CONNECTIVITY_ERROR.toString(),
                VIEW_TYPE_CONNECTIVITY_ERROR.toString(),
                ICON_CONNECTIVITY_ERROR.toString(),
                SUGGESTION_TEXT_CONNECTIVITY_ERROR))
            binding.searchView.suggestionsAdapter.changeCursor(this)
        }
    }

    private fun setAddressSuggestionsCursor(results: Collection<AddressWrapper>) {
        val columns: Array<String> = arrayOf(
                _ID,
                COLUMN_VIEW_TYPE,
                COLUMN_ICON,
                COLUMN_LATITUDE,
                COLUMN_LONGITUDE,
                SUGGEST_COLUMN_TEXT_1,
        )
        MatrixCursor(columns).apply {
            results.forEachIndexed { index, location ->
                val viewType: Int = if (location.recentQuery) {
                    VIEW_TYPE_LOCATION_RECENT_QUERY
                } else VIEW_TYPE_LOCATION_SUGGESTION

                val icon: Int = if (location.recentQuery) {
                    ICON_LOCATION_RECENT_QUERY
                } else ICON_LOCATION_SUGGESTION

                addRow(arrayOf(
                    index.toString(),
                    viewType.toString(),
                    icon.toString(),
                    location.latitude.toString(),
                    location.longitude.toString(),
                    location.address
                ))
            }
            binding.searchView.suggestionsAdapter.changeCursor(this)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun setLocationSuggestions(query: String?) {
        lifecycleScope.launch {
            val suggestions: List<AddressWrapper> = viewModel.getSuggestions(query)
            if (query.isNullOrEmpty()) {
                setAddressSuggestionsCursor(suggestions)
            } else {
                val addresses: List<AddressWrapper>? = try {
                    withContext(Dispatchers.IO) {
                        geocoder.getFromLocationName(query, 30)?.map {
                            AddressWrapper(
                                it.latitude,
                                it.longitude,
                                it.getAddressLine(0)
                            )
                        }
                    }
                } catch (e: IOException) {
                    setConnectionErrorCursor()
                    null
                }
                if (addresses != null) {
                    HashSet<AddressWrapper>().apply {

                        addAll(suggestions)
                        addAll(addresses)

                        setAddressSuggestionsCursor(this)
                    }
                } else if (suggestions.isNotEmpty()) {
                    setAddressSuggestionsCursor(suggestions)
                }
            }
        }
    }

    private fun setSearchViewSuggestions() {
        binding.searchView.suggestionsAdapter = SuggestionsAdapter(this, null)
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!isSuggestionClicked) {
                    setLocationSuggestions(newText)
                    return true
                }
                return false
            }
        })
    }

    private fun getMap() {
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun startMarkerAnimation() {

        val handler: Handler = Handler(Looper.getMainLooper())
        val start: Long = SystemClock.uptimeMillis()
        val duration: Int = 1500

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

    private fun updatePosition(latLng: LatLng) {
        viewModel.latLng.value = Pair(
            latLng, true
        )
    }

    private fun updateCameraBounds(latLng: LatLng, radius: Float, animate: Boolean) {
        MapsUtil.getLatLngBoundsFromCircle(latLng, radius).also { bounds ->
            if (animate) {
                mMap?.animateCamera(newLatLngBounds(bounds, 0))
            } else {
                mMap?.moveCamera(newLatLngBounds(bounds, 0))
            }
        }
    }

    private fun updateCameraBounds(animate: Boolean) {
        circle?.let { circle ->
            MapsUtil.getLatLngBoundsFromCircle(circle).also { bounds ->
                if (animate) {
                    mMap?.animateCamera(newLatLngBounds(bounds, 0))
                } else {
                    mMap?.moveCamera(newLatLngBounds(bounds, 0))
                }
            }
        }
    }

    private fun updatePosition(latLng: LatLng, queryAddress: Boolean) {
        addMarker(latLng)
        addCircle(latLng)
        mMap?.also {
            updateCameraBounds(MapsUtil.isTargetWithinVisibleRegion(
                it, latLng
            ))
        }
        if (queryAddress) getAddress(latLng) else marker?.title = viewModel.address.value
        startMarkerAnimation()
    }

    private fun setAddress(addressLine: String) {
        viewModel.setAddress(addressLine)
        marker?.title = addressLine
    }

    private fun setMapStyle(style: Int) {
        mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(
            this@MapsActivity, style
        ))
    }

    private fun getAddress(latLng: LatLng) {
        lifecycleScope.launch {
            val addresses: List<Address>? = withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                } catch (e: IOException) {
                    null
                }
            }
            if (!addresses.isNullOrEmpty()) {
                setAddress(addresses[0].getAddressLine(0))
            }
        }
    }

    private fun addCircle(latLng: LatLng) {
        circle?.remove()
        circle = mMap?.addCircle(CircleOptions()
                .center(latLng)
                .radius(viewModel.getRadius().toDouble())
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.teal_700))
    }

    private fun addMarker(latLng: LatLng) {
        marker?.remove()
        marker = mMap?.addMarker(MarkerOptions().position(latLng))
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceManager.REQUEST_ENABLE_LOCATION_SERVICES) {
            if (resultCode == Activity.RESULT_OK) {
                requestCurrentLocation()
            } else {
                onLocationRequestFailure()
            }
        }
    }

    @Suppress("MissingPermission")
    override fun onLocationRequestSuccess() {
        requestCurrentLocation()
    }

    override fun onLocationRequestFailure() {
        showSnackbar(
            findViewById(android.R.id.content),
            "Enable location services",
            LENGTH_LONG)
    }

    private fun requestCurrentLocation() {
        if (checkPermission(ACCESS_FINE_LOCATION)) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getCurrentLocation() {
        fusedLocationProvider.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, taskCancellationSource.token)
            .addOnCanceledListener {
                Log.w("MapsActivity", "Location request was cancelled")
            }
            .addOnSuccessListener { location ->
                if (location != null) updatePosition(LatLng(location.latitude, location.longitude))
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap?.setOnInfoWindowLongClickListener(this)
        mMap?.setOnMapClickListener(this)
        mMap?.setOnCameraMoveStartedListener(this)
        mMap?.setOnMarkerDragListener(this)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.events.collect {
                        when (it) {
                            is ShowMapStylesDialog -> showMapStylesDialog()
                            is ObtainCurrentLocation -> onLocationRequest()
                            is ToggleFloatingActionMenu -> toggleFloatingActionMenuVisibility()
                            is OnUpdateGeofenceEvent -> onUpdate(it.location)
                            is OnInsertGeofenceEvent -> onInsert(it.location)
                            is OnMapStyleChanged -> setMapStyle(it.style)
                            else -> Log.i("MapsActivity", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.latLng.collect { latLng ->
                        updatePosition(latLng.first, latLng.second)
                    }
                }
                launch {
                    viewModel.radius.collect {
                        circle?.radius = it.toDouble()
                        updateCameraBounds(true)
                    }
                }
                launch {
                    viewModel.profilesStateFlow.collect {
                        if (it.isNotEmpty()) {
                            profiles = it
                            setEntity()
                        }
                    }
                }
            }
        }
    }

    override fun onInfoWindowLongClick(p0: Marker) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Address", marker?.title))
        showSnackbar(findViewById(android.R.id.content), "Copied to clipboard", Snackbar.LENGTH_LONG)
    }

    override fun onMapClick(latLng: LatLng) {
        updatePosition(latLng)
    }

    override fun onCameraMoveStarted(reason: Int) {
        if (reason == REASON_GESTURE) {
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    override fun onMarkerDragStart(marker: Marker) {
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    override fun onMarkerDrag(p0: Marker) {

    }

    override fun onMarkerDragEnd(marker: Marker) {
        updatePosition(LatLng(marker.position.latitude, marker.position.longitude))
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state != STATE_HIDDEN) {
            bottomSheetBehavior.state = STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }

    override fun onTransformationFinished() {
        floatingMenuVisible = floatingActionMenuController.isVisible
    }

    companion object {

        private const val EXTRA_FLOATING_ACTION_MENU_VISIBLE: String = "menu"
        private const val BOTTOM_SHEET_OFFSET: Float = 100f
        private const val EXTRA_LOCATION_RELATION: String = "location"

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                locationRelation?.let {
                    putExtra(EXTRA_LOCATION_RELATION, it)
                }
            }
        }
    }
}