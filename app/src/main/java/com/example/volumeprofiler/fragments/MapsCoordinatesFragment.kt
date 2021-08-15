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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil
import com.example.volumeprofiler.util.TextUtil.Companion.filterCoordinatesInput
import com.example.volumeprofiler.util.TextUtil.Companion.filterRadiusInput
import com.example.volumeprofiler.util.TextUtil.Companion.filterStreetAddressInput
import com.example.volumeprofiler.util.TextUtil.Companion.validateCoordinatesInput
import com.example.volumeprofiler.util.TextUtil.Companion.validateRadiusInput
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.util.animations.Scale
import com.example.volumeprofiler.viewmodels.MapsCoordinatesViewModel
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsCoordinatesFragment: Fragment(), TextView.OnEditorActionListener {

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
    private var radiusEditText: EditText? = null
    private var progressTextView: TextView? = null
    private var slider: Slider? = null
    private var setRadiusButton: ImageButton? = null

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()
    private val localViewModel: MapsCoordinatesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = requireActivity() as Callback
    }

    override fun onDetach() {
        parentActivity = null
        super.onDetach()
    }

    /**
         * Calculates Y offset of target view relative to parent and converts it to value between 0.0F and 1.0F
         * @param targetView view, which Y offset will be calculated
     */
    private fun calculateHalfExpandedRatio(targetView: View): Float {
        val windowManager: WindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val defaultDisplay: Display = windowManager.defaultDisplay
        val displayMetrics: DisplayMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(displayMetrics)
        val rect: Rect = Rect()
        val rootViewGroup: ViewGroup = requireParentFragment().requireView().findViewById(R.id.coordinatorLayout)
        rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
        return (rect.top * 100F / rootViewGroup.height) / 100
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
    ): View {
        sharedViewModel.addressLine.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                Log.i(LOG_TAG, "observing address line")
                addressEditText.setText(it)
            }
        })
        sharedViewModel.latLng.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                Log.i(LOG_TAG, "observing latlng")
                latitudeEditText.setText(it.latitude.toString())
                longitudeEditText.setText(it.longitude.toString())
            }
        })
        return inflater.inflate(R.layout.maps_select_location_fragment, container, false)
    }

    private fun addTextObservers(): Unit {
        latitudeEditText.addTextChangedListener(getTextWatcher(latitudeTextInputLayout))
        longitudeEditText.addTextChangedListener(getTextWatcher(longitudeTextInputLayout))
        addressEditText.addTextChangedListener(getTextWatcher(addressTextInputLayout))
    }

    private fun setTextFilters(): Unit {
        val coordinatesInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend -> filterCoordinatesInput(source) }
        val streetAddressInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->  filterStreetAddressInput(source) }
        val radiusInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->  filterRadiusInput(source) }
        val coordinateFilters: Array<InputFilter> = arrayOf(coordinatesInputFilter)
        latitudeEditText.filters = coordinateFilters
        longitudeEditText.filters = coordinateFilters
        addressEditText.filters = arrayOf(streetAddressInputFilter)
        radiusEditText?.filters = arrayOf(radiusInputFilter)
    }

    private fun getTextWatcher(editTextLayout: TextInputLayout?): TextWatcher {
        return object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (editTextLayout?.id == R.id.addressTextInputLayout) {
                    localViewModel.address = s.toString()
                    editTextLayout.error = null
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                when (editTextLayout?.id) {
                    R.id.addressTextInputLayout -> localViewModel.address = s.toString()

                    R.id.longitudeTextInputLayout -> {
                        localViewModel.longitude = s.toString()
                        if (validateCoordinatesInput(s)) {
                            editTextLayout.error = null
                        } else {
                            editTextLayout.error = COORDINATES_EDIT_TEXT_ERROR
                        }
                    }
                    R.id.latitudeTextInputLayout -> {
                        localViewModel.latitude = s.toString()
                        if (validateCoordinatesInput(s)) {
                            editTextLayout.error = null
                        } else {
                            editTextLayout.error = COORDINATES_EDIT_TEXT_ERROR
                        }
                    }
                    R.id.radiusTextInputLayout -> {
                        localViewModel.radius = s.toString()
                        if (validateRadiusInput(s, localViewModel.metrics)) {
                            editTextLayout.error = null
                        } else {
                            editTextLayout.error = RADIUS_EDIT_TEXT_ERROR
                        }
                    }
                    else -> {
                        if (validateRadiusInput(s, localViewModel.metrics)) {
                            radiusEditText?.error = null
                        } else {
                            radiusEditText?.error = "Specify value within range from 100m to 100km"
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
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

    private fun hideSoftInputFromWindow(): Unit {
        val inputManager: InputMethodManager = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
    }

    private fun reverseAddress(): Unit {
        lifecycleScope.launch {
            val latLng: LatLng? = getCoordinatesFromAddress()
            if (latLng != null) {
                hideSoftInputFromWindow()
                (parentActivity as? BottomSheetFragment.Callbacks)?.collapseBottomSheet()
                sharedViewModel.animateCameraMovement = false
                sharedViewModel.setLatLng(latLng)
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
        transitionSet.addTransition(Scale()).addTarget(R.id.radiusSlider).addTarget(R.id.progressText)
        transitionSet.addTransition(Slide(Gravity.START)).addTarget(R.id.radiusEditText).addTarget(R.id.setRadiusButton)
        TransitionManager.go(secondScene, transitionSet)
    }

    private fun toSeekBarScene(sceneRoot: ViewGroup, listener: TransitionListenerAdapter): Unit {
        val transitionSet: TransitionSet = TransitionSet().addListener(listener)
        val firstScene = Scene.getSceneForLayout(sceneRoot, R.layout.seekbar_scene, requireContext())
        transitionSet.ordering = TransitionSet.ORDERING_SEQUENTIAL
        transitionSet.addTransition(Slide(Gravity.START)).addTarget(R.id.radiusEditText).addTarget(R.id.setRadiusButton)
        transitionSet.addTransition(Scale()).addTarget(R.id.radiusSlider).addTarget(R.id.progressText)
        TransitionManager.go(firstScene, transitionSet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sceneRoot: ViewGroup = view.findViewById(R.id.scene_root)

        val setLocationButton: Button = view.findViewById(R.id.setLocationButton)
        val setAddressButton: Button = view.findViewById(R.id.setAddressButton)
        setRadiusButton = view.findViewById(R.id.setRadiusButton)
        val toFirstScene: ImageButton = view.findViewById(R.id.toSeekbarScene)

        toSecondSceneButton = view.findViewById(R.id.toEditTextScene)
        toSecondSceneButton.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                toSecondSceneButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val parentFragmentView: View = requireParentFragment().requireView()
                parentActivity?.setHalfExpandedRatio(ViewUtil.calculateHalfExpandedRatio(
                    requireContext(),
                    parentFragmentView.findViewById(R.id.bottomSheetRoot),
                    toSecondSceneButton))
            }
        })
        latitudeTextInputLayout = view.findViewById(R.id.latitudeTextInputLayout)
        longitudeTextInputLayout = view.findViewById(R.id.longitudeTextInputLayout)
        addressTextInputLayout = view.findViewById(R.id.addressTextInputLayout)
        latitudeEditText = view.findViewById(R.id.latitudeTextInput)
        longitudeEditText = view.findViewById(R.id.longitudeEditText)
        addressEditText = view.findViewById(R.id.addressEditText)

        progressTextView = view.findViewById(R.id.progressText)
        slider = view.findViewById(R.id.radiusSlider)

        val spinner: Spinner = view.findViewById(R.id.metricsSpinner)
        spinner.adapter = ArrayAdapter<Metrics>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(Metrics.METERS, Metrics.KILOMETERS))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                localViewModel.metrics = parent?.selectedItem as Metrics
                updateMetrics()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        setTextFilters()
        addTextObservers()
        val toSeekBarSceneListener: TransitionListenerAdapter = object : TransitionListenerAdapter() {

            override fun onTransitionStart(transition: Transition) {
                super.onTransitionStart(transition)
                toFirstScene.setColorFilter(Color.GRAY)
                toSecondSceneButton.setColorFilter(Color.parseColor("#FF6B13EA"))

                slider = requireView().findViewById(R.id.radiusSlider)
                progressTextView = requireView().findViewById(R.id.progressText)
                setSliderOnChangeListener()
                updateMetrics()
            }
        }
        val toEditTextSceneListener: TransitionListenerAdapter = object : TransitionListenerAdapter() {

            override fun onTransitionStart(transition: Transition) {
                super.onTransitionStart(transition)
                toFirstScene.setColorFilter(Color.parseColor("#FF6B13EA"))
                toSecondSceneButton.setColorFilter(Color.GRAY)

                radiusEditText = requireView().findViewById(R.id.radiusEditText)
                setRadiusButton = requireView().findViewById(R.id.setRadiusButton)
                setRadiusButtonClickListener()
                setTextFilters()
                updateMetrics()
            }
        }
        val onClickListener: View.OnClickListener = View.OnClickListener {
            when (it.id) {
                setLocationButton.id -> {
                    var isInputValid: Boolean = true
                    if (!TextUtil.validateCoordinatesInput(latitudeEditText.text)) {
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
                        sharedViewModel.setLatLng(latLng)
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
        setSliderOnChangeListener()
        setRadiusButtonClickListener()
    }

    private fun updateMetrics(): Unit {
        if (localViewModel.metrics == Metrics.KILOMETERS) {
            val value = sharedViewModel.getRadius()!! / 1000
            if (slider != null) {
                slider!!.valueTo = Metrics.KILOMETERS.sliderMaxValue
                slider!!.valueFrom = Metrics.KILOMETERS.sliderMinValue
                slider!!.value = value
                progressTextView!!.text = "%.3f".format(value)
            }
            radiusEditText?.text = SpannableStringBuilder(("%.3f".format(value)))
        } else {
            val value: Float = if (sharedViewModel.getRadius()!! > Metrics.METERS.sliderMaxValue) {
                Metrics.METERS.sliderMaxValue
            } else {
                sharedViewModel.getRadius()!!
            }
            if (slider != null) {
                slider!!.valueTo = Metrics.METERS.sliderMaxValue
                slider!!.valueFrom = Metrics.METERS.sliderMinValue
                slider!!.value = value
                progressTextView!!.text = "%.3f".format(value)
            }
            radiusEditText?.text = SpannableStringBuilder(("%.3f".format(value)))
        }
    }

    private fun onSliderProgressChanged(progress: Float): Unit {
        progressTextView?.text = "%.3f".format(progress)
        if (localViewModel.metrics == Metrics.METERS) {
            sharedViewModel.setRadius(progress)
        }
        else {
            sharedViewModel.setRadius(progress * 1000)
        }
    }

    private fun setSliderOnChangeListener(): Unit {
        slider?.addOnChangeListener { slider, value, fromUser ->
            onSliderProgressChanged(value)
        }
    }

    private fun setRadiusButtonClickListener(): Unit {
        setRadiusButton?.setOnClickListener {
            if (validateRadiusInput(radiusEditText?.text, localViewModel.metrics)) {
                val value: Float = radiusEditText?.text.toString().toFloat()
                if (localViewModel.metrics == Metrics.KILOMETERS) {
                    sharedViewModel.setRadius(value * 1000)
                } else {
                    sharedViewModel.setRadius(value)
                }
            } else {
                Log.i(LOG_TAG, "input invalid")
            }
        }
    }

    companion object {
        private const val COORDINATES_EDIT_TEXT_ERROR: String = "Enter a correct value"
        private const val RADIUS_EDIT_TEXT_ERROR: String = "Enter a value within range from 100m to 100km"
        private const val LOG_TAG: String = "MapsCoordinatesFragment"
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            v?.clearFocus()
            return true
        }
        return false
    }
}