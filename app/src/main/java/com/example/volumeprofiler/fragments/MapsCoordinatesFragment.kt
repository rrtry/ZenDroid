package com.example.volumeprofiler.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.util.animations.Scale
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsCoordinatesFragment: Fragment() {

    private var parentActivity: Callback? = null

    interface Callback {

        fun setHalfExpandedRatio(ratio: Float): Unit

        fun getPeekHeight(): Int
    }

    private lateinit var latitudeTextInputLayout: TextInputLayout
    private lateinit var longitudeTextInputLayout: TextInputLayout
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var addressTextInputLayout: TextInputLayout
    private lateinit var toSecondSceneButton: ImageView

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentActivity = requireActivity() as Callback
        setTransitions()
    }

    private fun setTransitions(): Unit {
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
        enterTransition = inflater.inflateTransition(R.transition.slide_from_left)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        sharedViewModel.addressLine.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                addressEditText.setText(savedInstanceState?.getString(KEY_ADDRESS) ?: it)
            }
        })
        sharedViewModel.latLng.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                latitudeEditText.setText(savedInstanceState?.getString(KEY_LATITUDE)?: it.latitude.toString())
                longitudeEditText.setText(savedInstanceState?.getString(KEY_LONGITUDE)?: it.longitude.toString())
            }
        })
        sharedViewModel.bottomSheetState.observe(viewLifecycleOwner, Observer {
            if (it == BottomSheetBehavior.STATE_COLLAPSED) {
                removeTextObservers()
            } else {
                addTextObservers()
            }
        })
        return inflater.inflate(R.layout.maps_select_location_fragment, container, false)
    }

    private fun removeTextObservers(): Unit {
        longitudeEditText.removeTextChangedListener(getTextWatcher(longitudeTextInputLayout))
        latitudeEditText.removeTextChangedListener(getTextWatcher(latitudeTextInputLayout))
    }

    private fun addTextObservers(): Unit {
        latitudeEditText.addTextChangedListener(getTextWatcher(latitudeTextInputLayout))
        longitudeEditText.addTextChangedListener(getTextWatcher(longitudeTextInputLayout))
        addressEditText.addTextChangedListener(getTextWatcher(addressTextInputLayout))
    }

    private fun setTextFilters(): Unit {
        val coordinatesInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend -> filterCoordinatesInput(source) }
        val streetAddressInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->  filterStreetAddressInput(source) }
        val coordinateFilters: Array<InputFilter> = arrayOf(coordinatesInputFilter)
        val streetAddressFilters: Array<InputFilter> = arrayOf(streetAddressInputFilter)
        latitudeEditText.filters = coordinateFilters
        longitudeEditText.filters = coordinateFilters
        addressEditText.filters = streetAddressFilters
    }

    private fun getTextWatcher(editTextLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (editTextLayout.id == R.id.addressTextInputLayout) {
                    editTextLayout.error = null
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (editTextLayout.id != R.id.addressTextInputLayout) {
                    if (validateCoordinatesInput(s)) {
                        editTextLayout.error = null
                    } else {
                        editTextLayout.error = ERROR_TEXT
                    }
                }

            }

            override fun afterTextChanged(s: Editable?) { }
        }
    }

    private fun validateCoordinatesInput(source: CharSequence?): Boolean {
        return if (source != null && source.isNotEmpty()) {
            val double: Double? = source.toString().toDoubleOrNull()
            double != null
        } else {
            false
        }
    }

    private fun filterCoordinatesInput(source: CharSequence?): CharSequence {
        return if (source != null) {
            val stringBuilder: StringBuilder = StringBuilder()
            for (i in source) {
                if (Character.isDigit(i) || i == '.' || i == '-') {
                    stringBuilder.append(i)
                }
            }
            stringBuilder.toString()
        } else {
            ""
        }
    }

    private fun filterStreetAddressInput(source: CharSequence?): CharSequence {
        return if (source != null) {
            val stringBuilder: StringBuilder = StringBuilder()
            for (i in source) {
                if (Character.isLetter(i) || Character.isDigit(i) || i == ',' || i == '.' || i == '-' || Character.isSpaceChar(i)) {
                    stringBuilder.append(i)
                }
            }
            return stringBuilder.toString()
        } else {
            ""
        }
    }

    private suspend fun getCoordinatesFromAddress(): LatLng? {
        return if (addressEditText.text.isNotEmpty()) {
            val gc: Geocoder = Geocoder(requireContext(), Locale.getDefault())
            withContext(Dispatchers.IO) {
                val addressList: List<Address>? = gc.getFromLocationName(addressEditText.text.toString(), 1)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address: Address = addressList[0]
                    LatLng(address.latitude, address.longitude)
                } else {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun reverseAddress(): Unit {
        lifecycleScope.launch {
            val latLng: LatLng? = getCoordinatesFromAddress()
            if (latLng != null) {
                sharedViewModel.animateCameraMovement = false
                sharedViewModel.latLng.value = latLng
            } else {
                addressTextInputLayout.error = "Could not reverse specified address"
                AnimUtil.shakeAnimation(addressTextInputLayout)
            }
        }
    }

    private fun toEditTextScene(sceneRoot: ViewGroup, listener: TransitionListenerAdapter): Unit {
        val transitionSet: TransitionSet = TransitionSet().addListener(listener)
        val secondScene = Scene.getSceneForLayout(sceneRoot, R.layout.edit_text_scene, requireContext())
        transitionSet.ordering = TransitionSet.ORDERING_SEQUENTIAL
        transitionSet.addTransition(Scale()).addTarget(R.id.radiusSeekbar).addTarget(R.id.progressText)
        transitionSet.addTransition(Slide(Gravity.START)).addTarget(R.id.editText).addTarget(R.id.setRadiusButton)
        TransitionManager.go(secondScene, transitionSet)
    }

    private fun toSeekBarScene(sceneRoot: ViewGroup, listener: TransitionListenerAdapter): Unit {
        val transitionSet: TransitionSet = TransitionSet().addListener(listener)
        val firstScene = Scene.getSceneForLayout(sceneRoot, R.layout.seekbar_scene, requireContext())
        transitionSet.ordering = TransitionSet.ORDERING_SEQUENTIAL
        transitionSet.addTransition(Slide(Gravity.START)).addTarget(R.id.editText).addTarget(R.id.setRadiusButton)
        transitionSet.addTransition(Scale()).addTarget(R.id.radiusSeekbar).addTarget(R.id.progressText)
        TransitionManager.go(firstScene, transitionSet)
    }

    /*
        The method converts y offset relative to parent to ratio value between 0.0 and 1.0
     */
    private fun calculateHalfExpandedRatio(): Float {
        val windowManager: WindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val defaultDisplay: Display = windowManager.defaultDisplay
        val displayMetrics: DisplayMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(displayMetrics)
        val rootViewGroup: ViewGroup = requireParentFragment().requireView().findViewById<ViewGroup>(R.id.bottomSheetRoot)
        val rect: Rect = Rect()
        rootViewGroup.offsetDescendantRectToMyCoords(toSecondSceneButton, rect)
        return (rect.top * 100 / rootViewGroup.height).toFloat() / 100
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sceneRoot: ViewGroup = view.findViewById(R.id.scene_root)

        val setLocationButton: Button = view.findViewById(R.id.setLocationButton)
        val setAddressButton: Button = view.findViewById(R.id.setAddressButton)
        val toFirstScene: ImageButton = view.findViewById(R.id.toSeekbarScene)
        toSecondSceneButton = view.findViewById(R.id.toEditTextScene)
        toSecondSceneButton.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                toSecondSceneButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                parentActivity?.setHalfExpandedRatio(calculateHalfExpandedRatio())
            }
        })
        latitudeTextInputLayout = view.findViewById(R.id.latitudeTextInputLayout)
        longitudeTextInputLayout = view.findViewById(R.id.longitudeTextInputLayout)
        addressTextInputLayout = view.findViewById(R.id.addressTextInputLayout)
        latitudeEditText = view.findViewById(R.id.latitudeTextInput)
        longitudeEditText = view.findViewById(R.id.radiusTextInput)
        addressEditText = view.findViewById(R.id.addressTextInput)
        val progressTextView: TextView = view.findViewById(R.id.progressText)
        val seekBar: SeekBar = view.findViewById(R.id.radiusSeekbar)

        setTextFilters()
        addTextObservers()
        val toSeekBarSceneListener: TransitionListenerAdapter = object : TransitionListenerAdapter() {

            override fun onTransitionStart(transition: Transition) {
                super.onTransitionStart(transition)
                toFirstScene.setColorFilter(Color.GRAY)
                toSecondSceneButton.setColorFilter(Color.parseColor("#FF6B13EA"))
            }
        }
        val toEditTextSceneListener: TransitionListenerAdapter = object : TransitionListenerAdapter() {

            override fun onTransitionStart(transition: Transition) {
                super.onTransitionStart(transition)
                toFirstScene.setColorFilter(Color.parseColor("#FF6B13EA"))
                toSecondSceneButton.setColorFilter(Color.GRAY)
            }
        }
        val onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar!!.progress < 100 && fromUser) {
                    seekBar.progress = 100
                } else {
                    seekBar.progress = progress
                }
                progressTextView.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.i(LOG_TAG, "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.i(LOG_TAG, "onStopTrackingTouch")
            }
        }
        val onClickListener: View.OnClickListener = View.OnClickListener {
            when (it.id) {
                setLocationButton.id -> {
                    var isInputValid: Boolean = true
                    if (!validateCoordinatesInput(latitudeEditText.text)) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(latitudeTextInputLayout)
                    }
                    if (!validateCoordinatesInput(longitudeEditText.text)) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(longitudeTextInputLayout)
                    }
                    if (isInputValid) {
                        val latLng: LatLng = LatLng(latitudeEditText.text.toString().toDouble(),
                                longitudeEditText.text.toString().toDouble())
                        sharedViewModel.animateCameraMovement = false
                        sharedViewModel.latLng.value = latLng
                    }
                }
                toFirstScene.id -> {
                    toSeekBarScene(sceneRoot, toSeekBarSceneListener)
                }
                toSecondSceneButton.id -> {
                    toEditTextScene(sceneRoot, toEditTextSceneListener)
                }
                setAddressButton.id -> {
                    reverseAddress()
                }
            }
        }
        setLocationButton.setOnClickListener(onClickListener)
        toFirstScene.setOnClickListener(onClickListener)
        toSecondSceneButton.setOnClickListener(onClickListener)
        setAddressButton.setOnClickListener(onClickListener)
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
    }

    companion object {
        private const val KEY_ADDRESS: String = "key_string"
        private const val KEY_LATITUDE: String = "key_latitude"
        private const val KEY_LONGITUDE: String = "key_longitude"
        private const val ERROR_TEXT: String = "Enter a correct value"
        private const val LOG_TAG: String = "MapsCoordinatesFragment"
    }
}