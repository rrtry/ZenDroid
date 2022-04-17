package com.example.volumeprofiler.activities

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.SearchManager.SUGGEST_COLUMN_TEXT_1
import android.content.*
import android.content.Intent.ACTION_SEARCH
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import android.os.*
import android.provider.BaseColumns._ID
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.GoogleMapsActivityBinding
import com.example.volumeprofiler.databinding.LocationSuggestionItemViewBinding
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.fragments.BottomSheetFragment
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_ICON
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LATITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LONGITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_VIEW_TYPE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_EMPTY_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ICON_LOCATION_SUGGESTION
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ID_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.ID_EMPTY_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.SUGGESTION_TEXT_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.SUGGESTION_TEXT_EMPTY_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_EMPTY_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_LOCATION_SUGGESTION
import com.example.volumeprofiler.util.ViewUtil.Companion.convertDipToPx
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.example.volumeprofiler.util.ui.animations.SimpleAnimationListener
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel
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
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import kotlin.math.ln
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel.ViewEvent.*
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Runnable

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        DetailsViewContract<Location>,
        GeofenceManager.LocationRequestListener {

    @SuppressLint("Range")
    private class SuggestionsAdapter(context: Context, cursor: MatrixCursor?): CursorAdapter(context, cursor, 0) {

        private class ViewHolder {

            lateinit var suggestionIcon: ImageView
            lateinit var suggestionText: TextView
        }

        private val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
            val suggestionBinding = LocationSuggestionItemViewBinding.inflate(inflater)

            ViewHolder().apply {

                suggestionText = suggestionBinding.locationName
                suggestionIcon = suggestionBinding.locationIcon

                suggestionBinding.root.tag = this
            }
            return suggestionBinding.root
        }

        override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
            cursor?.let {
                val viewHolder = view?.tag as ViewHolder

                val iconRes: Int = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_ICON)))
                val suggestionText: String = cursor.getString(cursor.getColumnIndex(SUGGEST_COLUMN_TEXT_1))

                viewHolder.suggestionIcon.setImageDrawable(AppCompatResources.getDrawable(context!!, iconRes))
                viewHolder.suggestionText.text = suggestionText
            }
        }
    }

    interface ItemSelectedListener {

        fun onItemSelected(itemId: Int)
    }

    private val viewModel: GeofenceSharedViewModel by viewModels()

    private lateinit var job: Job
    private lateinit var coroutineScope: CoroutineScope

    private var marker: Marker? = null
    private var circle: Circle? = null

    private var bindingImpl: GoogleMapsActivityBinding? = null
    private val binding: GoogleMapsActivityBinding get() = bindingImpl!!

    private var isAnimationRunning: Boolean = false
    private var isOverlayVisible: Boolean = false
    private var isSuggestionClicked: Boolean = false
    private var activeNetworkHasInet: Boolean = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mMap: GoogleMap
    private lateinit var profiles: List<Profile>
    private lateinit var savedLocations: List<AddressWrapper>
    private lateinit var taskCancellationSource: CancellationTokenSource
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            showNetworkStateMessage()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            showNetworkStateMessage()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            showNetworkStateMessage()
        }
    }

    private fun showNetworkStateMessage() {
        if (hasInternetCapability(connectivityManager.activeNetwork) && !activeNetworkHasInet) {
            activeNetworkHasInet = true
            Log.i("NetworkCallback", "Internet connectivity restored")
        } else if (activeNetworkHasInet) {
            activeNetworkHasInet = false
            Log.i("NetworkCallback", "Internet connectivity lost")
        }
    }

    private fun hasInternetCapability(network: Network?): Boolean {
        connectivityManager.getNetworkCapabilities(network)?.let {
            return it.hasCapability(NET_CAPABILITY_INTERNET)
        }
        return false
    }

    override fun onUpdate(location: Location) {
        lifecycleScope.launch {
            viewModel.updateLocation(location)
            captureSnapshot(
                LatLng(location.latitude, location.longitude),
                location.previewImageId
            )
        }
    }

    override fun onInsert(location: Location) {
        lifecycleScope.launch {
            viewModel.addLocation(location)
            captureSnapshot(
                LatLng(location.latitude, location.longitude),
                location.previewImageId
            )
        }
    }

    override fun onCancel() {
        finish()
    }

    private fun onSnapshotReady(bitmap: Bitmap?, uuid: UUID) {
        lifecycleScope.launch {
            bitmap?.also {
                withContext(Dispatchers.IO) {
                    writeThumbnail(this@MapsActivity, uuid, it)
                }
            }
        }.invokeOnCompletion {
            finish()
        }
    }

    private fun captureSnapshot(latLng: LatLng, uuid: UUID) {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
            latLng, getZoomLevel()
        )))
        Handler(Looper.getMainLooper()).postDelayed( {
            mMap.clear()
            mMap.snapshot {
                onSnapshotReady(it, uuid)
            }
        }, 100)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                geofenceManager.checkLocationServicesAvailability(this)
            }
        }

        bindingImpl = GoogleMapsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        geocoder = Geocoder(this, Locale.getDefault())

        activeNetworkHasInet = hasInternetCapability(connectivityManager.activeNetwork)

        setSearchConfiguration()
        setBottomSheetBehavior()
        addBottomSheetFragment()
        getMap()

        binding.expandableFab.setOnClickListener {
            toggleOverlayMenuState()
        }
        binding.overlay.setOnClickListener {
            toggleOverlayMenuState()
        }
        binding.currentLocationFab.setOnClickListener {
            if (checkPermission(ACCESS_FINE_LOCATION)) {
                geofenceManager.checkLocationServicesAvailability(this)
            } else {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
        binding.overlayOptionsFab.setOnClickListener {

        }
        binding.bottomNavigation.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.bottomNavigation.viewTreeObserver.removeOnGlobalLayoutListener(this)
                bottomSheetBehavior.peekHeight = binding.bottomNavigation.height + convertDipToPx(BOTTOM_SHEET_OFFSET)
            }
        })
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

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.latLng.collect { latLng ->
                        if (latLng != null) {
                            setLocation(latLng.first, latLng.second)
                        } else {
                            setDefaultLocation()
                        }
                    }
                }
                launch {
                    viewModel.radius.collect {
                        circle?.radius = it.toDouble()
                        marker?.position?.let { position ->
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, getZoomLevel()))
                        }
                    }
                }
                launch {
                    viewModel.events.collect {
                        when (it) {
                            is OnWrongInputEvent -> {
                                showSnackbar(binding.root, "Fill all fields correctly", LENGTH_LONG)
                                bottomSheetBehavior.state = STATE_EXPANDED
                            }
                            is OnUpdateGeofenceEvent -> {
                                onUpdate(it.location)
                            }
                            is OnInsertGeofenceEvent -> {
                                onInsert(it.location)
                            }
                            else -> Log.i("MapsActivity", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.locationsFlow.map { locations ->
                        locations.map {
                            AddressWrapper(
                                it.location.latitude,
                                it.location.longitude,
                                it.location.address
                            )
                        }
                    }.collect {
                        savedLocations = it
                    }
                }
                launch {
                    viewModel.profilesStateFlow.collect {
                        profiles = it
                        setEntity()
                    }
                }
            }
        }
    }

    private fun toggleOverlayMenuState() {
        if (!isAnimationRunning) {

            isAnimationRunning = true

            binding.overlay.isClickable = !isOverlayVisible
            binding.overlay.isFocusable = !isOverlayVisible

            val views: List<View> = listOf(
                binding.overlay,
                binding.saveGeofenceButton,
                binding.saveChangesLabel,
                binding.overlayOptionsFab,
                binding.mapOverlayLabel,
                binding.currentLocationFab,
                binding.currentLocationLabel
            )
            if (isOverlayVisible) {
                AnimUtil.getFabCollapseAnimation(binding.expandableFab)
                    .start()
            } else {
                AnimUtil.getFabExpandAnimation(binding.expandableFab)
                    .start()
            }
            for ((index, view) in views.withIndex()) {
                if (view is ViewGroup) {
                    view.startAnimation(AnimUtil.getOverlayAnimation(view))
                } else {
                    view.startAnimation(
                        AnimUtil.getMenuOptionAnimation(binding.expandableFab, view).apply {
                            setAnimationListener(object : SimpleAnimationListener() {

                                override fun onAnimationEnd(animation: Animation?) {
                                    view.visibility = if (view.visibility == View.VISIBLE) {
                                        View.INVISIBLE
                                    } else {
                                        View.VISIBLE
                                    }
                                    if (index == views.size - 1) {
                                        isAnimationRunning = false
                                    }
                                }
                            })
                        })
                }
            }
            isOverlayVisible = !isOverlayVisible
        }
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

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior = from(binding.containerBottomSheet)
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.halfExpandedRatio = 0.5f
    }

    override fun onStart() {
        super.onStart()
        job = Job()
        coroutineScope = CoroutineScope(Dispatchers.Main + job)

        taskCancellationSource = CancellationTokenSource()
        connectivityManager.registerNetworkCallback(getNetworkRequest(), networkCallback)
    }

    override fun onStop() {
        super.onStop()
        job.cancel()

        taskCancellationSource.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
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

    private fun setSuggestionsAdapter() {
        binding.searchView.suggestionsAdapter = SuggestionsAdapter(this, null)
    }

    private fun setEmptyQueryCursor() {
        MatrixCursor(arrayOf(_ID, COLUMN_VIEW_TYPE, COLUMN_ICON, SUGGEST_COLUMN_TEXT_1)).apply {
            addRow(arrayOf(
                ID_EMPTY_QUERY.toString(),
                VIEW_TYPE_EMPTY_QUERY.toString(),
                ICON_EMPTY_QUERY.toString(),
                SUGGESTION_TEXT_EMPTY_QUERY))
            binding.searchView.suggestionsAdapter.changeCursor(this)
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

    private fun setAddressSuggestionsCursor(results: List<AddressWrapper>) {
        val columns: Array<String> = arrayOf(
                _ID,
                COLUMN_VIEW_TYPE,
                COLUMN_ICON,
                COLUMN_LATITUDE,
                COLUMN_LONGITUDE,
                SUGGEST_COLUMN_TEXT_1,
        )
        MatrixCursor(columns).apply {
            for (i in results.indices) {
                val row: Array<String> = arrayOf(
                    i.toString(),
                    VIEW_TYPE_LOCATION_SUGGESTION.toString(),
                    ICON_LOCATION_SUGGESTION.toString(),
                    results[i].latitude.toString(),
                    results[i].longitude.toString(),
                    results[i].address
                )
                addRow(row)
            }
            binding.searchView.suggestionsAdapter.changeCursor(this)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun getAddressList(query: String) {
        coroutineScope.launch {
            try {
                val addressList: List<AddressWrapper>? = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(query, 15)?.map {
                        AddressWrapper(
                            it.latitude,
                            it.longitude,
                            it.getAddressLine(0)
                        )
                    }
                }
                if (addressList.isNullOrEmpty()) {
                    setEmptyQueryCursor()
                } else {
                    setAddressSuggestionsCursor(addressList)
                }
            } catch (e: IOException) {
                Log.e("Geocoder", "$e - $query")
                setConnectionErrorCursor()
            }
        }
    }

    private fun setSearchViewSuggestions() {
        setSuggestionsAdapter()
        binding.searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {

            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(position: Int): Boolean {

                binding.searchView.suggestionsAdapter.cursor.apply {

                    val viewType: Int = Integer.parseInt(getString(getColumnIndex(COLUMN_VIEW_TYPE)))

                    if (viewType == VIEW_TYPE_LOCATION_SUGGESTION) {

                        isSuggestionClicked = true

                        viewModel.latLng.value = Pair(LatLng(
                            getString(getColumnIndex(COLUMN_LATITUDE)).toDouble(),
                            getString(getColumnIndex(COLUMN_LONGITUDE)).toDouble()
                        ), false)

                        getString(getColumnIndex(SUGGEST_COLUMN_TEXT_1)).let { address ->
                            viewModel.address.value = address
                            binding.searchView.setQuery(address, false)
                        }

                        isSuggestionClicked = false
                        return true
                    }
                }
                return false
            }
        })
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank() && !isSuggestionClicked) {
                    getAddressList(newText)
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

    private fun setNightStyle() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night))
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

    private fun targetWithinCameraBounds(latLng: LatLng): Boolean {
        return mMap.projection.visibleRegion.latLngBounds.contains(latLng)
    }

    private fun setDefaultLocation() {
        if (checkPermission(ACCESS_FINE_LOCATION)) {
            getRecentLocation()
        } else {
            getLocaleLocation()
        }
    }

    private fun setLocation(latLng: LatLng, queryAddress: Boolean) {
        addMarker(latLng)
        addCircle(latLng)
        getZoomLevel().also {
            if (targetWithinCameraBounds(latLng)) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, it))
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, it))
            }
        }
        if (queryAddress) {
            getAddress(latLng)
        } else {
            marker?.title = viewModel.address.value
        }
        startMarkerAnimation()
    }

    private fun setAddress(addressLine: String) {
        viewModel.setAddress(addressLine)
        marker?.title = addressLine
    }

    private fun getAddress(latLng: LatLng) {
        coroutineScope.launch {
            val addresses: List<Address>? = withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                } catch (e: IOException) {
                    Log.e("GeocoderUtil", "IOException: $latLng")
                    null
                }
            }
            if (!addresses.isNullOrEmpty()) {
                setAddress(addresses[0].getAddressLine(0))
            } else {
                Log.i("Geocoder", "Could not get address from location $latLng")
            }
        }
    }

    private fun getZoomLevel(): Float {
        circle?.let {
            // val maxZoomLevel: Int = 16
            val scale: Double = (it.radius + it.radius / 2) / 400
            return (mMap.maxZoomLevel - ln(scale) / ln(2.0)).toFloat()
        }
        return mMap.maxZoomLevel
    }

    private fun addCircle(latLng: LatLng) {
        circle?.remove()
        circle = mMap.addCircle(CircleOptions()
                .center(latLng)
                .radius(viewModel.getRadius().toDouble())
                .strokeColor(Color.TRANSPARENT)
                .fillColor(R.color.geofence_fill_color))
    }

    private fun addMarker(latLng: LatLng) {
        marker?.remove()
        marker = mMap.addMarker(MarkerOptions().position(latLng))
    }

    private fun getLocaleLocation() {
        coroutineScope.launch {
            val addresses: List<Address>? = withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocationName(Locale.getDefault().country, 1)
                } catch (e: IOException) {
                    Log.e("Geocoder", "IOException: ${Locale.getDefault().country}")
                    null
                }
            }
            if (addresses.isNullOrEmpty()) {
                updatePosition(LatLng(-33.865143, 151.209900))
            } else {
                addresses.first().also {
                    updatePosition(LatLng(it.latitude, it.longitude))
                }
            }
        }
    }

    @Suppress("MissingPermission")
    override fun onSuccess() {
        getCurrentLocation()
    }

    override fun onFailure() {
        showSnackbar(binding.root, "Failed to obtain current location", LENGTH_LONG)
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getRecentLocation() {
        fusedLocationProvider.lastLocation
            .addOnFailureListener {
                showSnackbar(binding.root, "Failed to fetch last known location", LENGTH_LONG)
            }.addOnSuccessListener {
                if (it != null) {
                    updatePosition(LatLng(it.latitude, it.longitude))
                } else {
                    getCurrentLocation()
                }
        }
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getCurrentLocation() {
        fusedLocationProvider.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, taskCancellationSource.token)
            .addOnCanceledListener {
                Log.w("MapsActivity", "Location request was cancelled")
            }
            .addOnSuccessListener { location ->
                if (location != null) {
                    updatePosition(LatLng(location.latitude, location.longitude))
                } else {
                    getLocaleLocation()
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapClickListener(this)
        mMap.setOnCameraMoveStartedListener(this)
        mMap.setOnMarkerDragListener(this)
    }

    override fun onMapClick(p0: LatLng) {
        updatePosition(p0)
    }

    override fun onCameraMoveStarted(p0: Int) {
        if (p0 == REASON_GESTURE) {
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    override fun onMarkerDragStart(p0: Marker) {
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    override fun onMarkerDrag(p0: Marker) {

    }

    override fun onMarkerDragEnd(p0: Marker) {
        updatePosition(LatLng(p0.position.latitude, p0.position.longitude))
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state != STATE_HIDDEN) {
            bottomSheetBehavior.state = STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        private const val BOTTOM_SHEET_OFFSET: Float = 100f
        private const val EXTRA_LOCATION_RELATION: String = "location"

        private fun getNetworkRequest(): NetworkRequest {
            return NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .addTransportType(TRANSPORT_WIFI)
                .addTransportType(TRANSPORT_CELLULAR)
                .build()
        }

        fun newIntent(context: Context, locationRelation: LocationRelation?): Intent {
            return Intent(context, MapsActivity::class.java).apply {
                locationRelation?.let {
                    putExtra(EXTRA_LOCATION_RELATION, it)
                }
            }
        }
    }
}