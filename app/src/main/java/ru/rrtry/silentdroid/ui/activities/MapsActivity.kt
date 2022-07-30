package ru.rrtry.silentdroid.ui.activities

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.*
import android.content.Intent.ACTION_SEARCH
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.rrtry.silentdroid.core.FileManager
import ru.rrtry.silentdroid.core.GeofenceManager
import ru.rrtry.silentdroid.core.GeofenceManager.Companion.ACCESS_LOCATION
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.entities.LocationSuggestion
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.ui.fragments.BottomSheetFragment
import ru.rrtry.silentdroid.ui.fragments.MapThemeSelectionDialog
import ru.rrtry.silentdroid.interfaces.DetailsViewContract
import ru.rrtry.silentdroid.util.ViewUtil.Companion.convertDipToPx
import ru.rrtry.silentdroid.util.ViewUtil.Companion.showSnackbar
import ru.rrtry.silentdroid.ui.FloatingActionMenuController
import ru.rrtry.silentdroid.viewmodels.GeofenceSharedViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import ru.rrtry.silentdroid.R
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import ru.rrtry.silentdroid.viewmodels.GeofenceSharedViewModel.ViewEvent.*
import ru.rrtry.silentdroid.ui.views.AddressSearchView
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import ru.rrtry.silentdroid.databinding.GoogleMapsActivityBinding
import ru.rrtry.silentdroid.util.*
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
        GoogleMap.OnInfoWindowLongClickListener,
        FloatingActionMenuController.MenuStateListener,
        AddressSearchView.OnSuggestionListener,
        AddressSearchView.OnQueryTextChangeListener {

    interface ItemSelectedListener {

        fun onItemSelected(itemId: Int)
    }

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var geocoderUtil: GeocoderUtil

    private val viewModel: GeofenceSharedViewModel by viewModels()

    private var mMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null

    private var bindingImpl: GoogleMapsActivityBinding? = null
    private val binding: GoogleMapsActivityBinding get() = bindingImpl!!

    private var floatingMenuVisible: Boolean = false
    private var elapsedTime: Long = 0

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var profiles: List<Profile>
    private lateinit var taskCancellationSource: CancellationTokenSource
    private lateinit var coarseLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var floatingActionMenuController: FloatingActionMenuController
    private lateinit var networkObserver: NetworkStateObserver

    override fun onSuggestionSelected(address: AddressWrapper) {
        viewModel.address.value = address.address
        viewModel.latLng.value = Pair(LatLng(address.latitude, address.longitude), false)
        if (!address.recentQuery) {
            viewModel.addSuggestion(
                LocationSuggestion(
                address.address,
                address.latitude,
                address.longitude
            )
            )
        }
        binding.searchView.setQuery(address.address, false)
        binding.searchView.closeSuggestions()
    }

    override fun onSuggestionRemoved(address: AddressWrapper) = Unit

    override fun onQueryTextChange(query: String?) {
        lifecycleScope.launch {
            val savedQueries: HashSet<AddressWrapper> = viewModel.getSuggestions(query).toHashSet()
            val results: List<AddressWrapper> = geocoderUtil.queryAddresses(query) ?: listOf()
            binding.searchView.updateAdapter((savedQueries + results).toList())
        }
    }

    override fun onQueryTextSubmit(query: String?) = Unit

    override fun onNetworkAvailable() {
        runOnUiThread {
            Toast.makeText(
                this,
                resources.getString(R.string.network_available),
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onNetworkLost() {
        runOnUiThread {
            Toast.makeText(
                this,
                resources.getString(R.string.network_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onUpdate(location: Location) {
        lifecycleScope.launch {
            viewModel.updateLocation(location)
        }.invokeOnCompletion {
            if (viewModel.isRegistered) {
                geofenceManager.addGeofence(
                    location,
                    viewModel.enterProfile.value!!,
                    viewModel.exitProfile.value!!
                )
            }
            onFinish(true)
        }
    }

    override fun onInsert(location: Location) {
        lifecycleScope.launch {
            viewModel.addLocation(location)
        }.invokeOnCompletion {
            onFinish(true)
        }
    }

    override fun onFinish(result: Boolean) {
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_FLOATING_ACTION_MENU_VISIBLE, floatingMenuVisible)
        outState.putLong(EXTRA_ELAPSED_TIME, elapsedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        elapsedTime = savedInstanceState.getLong(EXTRA_ELAPSED_TIME, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            floatingMenuVisible = it.getBoolean(EXTRA_FLOATING_ACTION_MENU_VISIBLE, false)
        }

        bindingImpl = GoogleMapsActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setEntity()

        networkObserver = NetworkStateObserver(WeakReference(this))
        floatingActionMenuController = FloatingActionMenuController(WeakReference(this), floatingMenuVisible)

        registerForPermissionRequestResult()
        setBottomSheetBehavior()
        setSearchListeners()
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
        coarseLocationPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                geofenceManager.checkLocationServicesAvailability(this)
            } else if (shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION)) {
                showSnackbar(
                    findViewById(android.R.id.content),
                    resources.getString(R.string.location_access_required),
                    Snackbar.LENGTH_INDEFINITE,
                    resources.getString(R.string.grant))
                {
                    coarseLocationPermissionLauncher.launch(ACCESS_COARSE_LOCATION)
                }
            } else {
                openPackageInfoActivity()
            }
        }
        backgroundLocationPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { map ->

            viewModel.backgroundLocationAccessGranted = geofenceManager.locationAccessGranted()

            if (!map.containsValue(false)) {
                viewModel.onApplyChangesButtonClick()
            } else if (shouldShowRequestPermissionRationale(ACCESS_LOCATION)) {
                showSnackbar(
                    findViewById(android.R.id.content),
                    resources.getString(R.string.snackbar_location_permission_explanation),
                    Snackbar.LENGTH_INDEFINITE,
                    resources.getString(R.string.grant))
                {
                    geofenceManager.requestLocationPermission(backgroundLocationPermissionLauncher)
                }
            } else {
                showSnackbar(
                    findViewById(android.R.id.content),
                    resources.getString(R.string.snackbar_location_permission_explanation),
                    Snackbar.LENGTH_INDEFINITE,
                    resources.getString(R.string.open_settings))
                {
                    openPackageInfoActivity()
                }
            }
        }
    }

    private fun onLocationRequest() {
        if (checkPermission(ACCESS_COARSE_LOCATION)) {
            geofenceManager.checkLocationServicesAvailability(this@MapsActivity)
        } else {
            coarseLocationPermissionLauncher.launch(ACCESS_COARSE_LOCATION)
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

    private fun addBottomSheetFragment() {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(R.id.containerBottomSheet)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.containerBottomSheet, BottomSheetFragment())
                .commit()
        }
    }

    private fun setSearchListeners() {
        binding.searchView.queryListener = this
        binding.searchView.adapter = AddressSearchView.AddressAdapter(this)
        binding.searchView.selectionListener = this
    }

    private fun setBottomNavigationListeners() {
        binding.bottomNavigation.doOnLayout {
            bottomSheetBehavior.peekHeight = binding.bottomNavigation.height + convertDipToPx(
                BOTTOM_SHEET_OFFSET
            )
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
    }

    override fun onStart() {
        super.onStart()
        viewModel.backgroundLocationAccessGranted = geofenceManager.locationAccessGranted()
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
        viewModel.latLng.value = Pair(latLng, true)
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
        addMarker(latLng, queryAddress)
        addCircle(latLng)
        mMap?.also {
            updateCameraBounds(
                MapsUtil.isTargetWithinVisibleRegion(
                it, latLng
            ))
        }
        startMarkerAnimation()
    }

    private fun setMapStyle(style: Int) {
        mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(
            this@MapsActivity, style
        ))
    }

    private fun setAddress(latLng: LatLng) {
        lifecycleScope.launch {
            geocoderUtil.getAddressFromLocation(latLng)?.let {
                viewModel.address.value = it
                marker?.title = it
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

    private fun addMarker(latLng: LatLng, queryAddress: Boolean) {
        marker?.remove()
        marker = mMap?.addMarker(MarkerOptions().position(latLng))
        if (queryAddress) {
            setAddress(latLng)
        } else {
            marker?.title = viewModel.address.value
        }
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
            resources.getString(R.string.enable_location_service),
            LENGTH_LONG)
    }

    private fun requestCurrentLocation() {
        if (checkPermission(ACCESS_COARSE_LOCATION)) {
            getCurrentLocation()
        } else {
            coarseLocationPermissionLauncher.launch(ACCESS_COARSE_LOCATION)
        }
    }

    @RequiresPermission(ACCESS_COARSE_LOCATION)
    private fun getCurrentLocation() {
        val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
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
                    viewModel.events.collect { event ->
                        when (event) {
                            is ShowMapStylesDialog -> showMapStylesDialog()
                            is ObtainCurrentLocation -> onLocationRequest()
                            is ToggleFloatingActionMenu -> toggleFloatingActionMenuVisibility()
                            is OnUpdateGeofenceEvent -> onUpdate(event.location)
                            is OnInsertGeofenceEvent -> onInsert(event.location)
                            is OnMapStyleChanged -> setMapStyle(event.style)
                            is OnRequestBackgroundLocationPermission -> geofenceManager.requestLocationPermission(backgroundLocationPermissionLauncher)
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
                    viewModel.profilesStateFlow.collect { it ->
                        if (it.isNotEmpty()) {
                            profiles = it
                            viewModel.setProfiles(it)
                        }
                    }
                }
            }
        }
    }

    private fun onMapInteraction() {
        bottomSheetBehavior.state = STATE_HIDDEN
        binding.searchView.closeSuggestions()
    }

    override fun onInfoWindowLongClick(p0: Marker) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, marker?.title))
        showSnackbar(findViewById(android.R.id.content), resources.getString(R.string.clipboard), Snackbar.LENGTH_LONG)
    }

    override fun onMapClick(latLng: LatLng) {
        updatePosition(latLng)
        onMapInteraction()
    }

    override fun onCameraMoveStarted(reason: Int) {
        if (reason == REASON_GESTURE) onMapInteraction()
    }

    override fun onMarkerDragStart(marker: Marker) {
        onMapInteraction()
    }

    override fun onMarkerDrag(p0: Marker) {
        onMapInteraction()
    }

    override fun onMarkerDragEnd(marker: Marker) {
        updatePosition(LatLng(marker.position.latitude, marker.position.longitude))
    }

    override fun onBackPressed() {
        if (binding.searchView.suggestionsVisible) {
            binding.searchView.closeSuggestions()
        } else if (bottomSheetBehavior.state != STATE_HIDDEN) {
            bottomSheetBehavior.state = STATE_HIDDEN
        } else {
            if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
                onFinish(false)
            } else {
                showSnackbar(findViewById(android.R.id.content), resources.getString(R.string.confirm_change_dismissal), LENGTH_LONG)
            }
            elapsedTime = System.currentTimeMillis()
        }
    }

    override fun onTransformationFinished() {
        floatingMenuVisible = floatingActionMenuController.isVisible
    }

    companion object {

        private const val EXTRA_FLOATING_ACTION_MENU_VISIBLE: String = "menu"
        private const val BOTTOM_SHEET_OFFSET: Float = 100f
        private const val EXTRA_ELAPSED_TIME: String = "elapsed"
        private const val EXTRA_LOCATION_RELATION: String = "location"
        private const val CLIPBOARD_LABEL: String = "Address"

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                locationRelation?.let {
                    putExtra(EXTRA_LOCATION_RELATION, it)
                }
            }
        }
    }
}