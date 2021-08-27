package com.example.volumeprofiler.fragments

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.MapsSelectLocationFragmentBinding
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil
import com.example.volumeprofiler.util.TextUtil.Companion.validateCoordinatesInput
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.util.animations.Scale
import com.example.volumeprofiler.viewmodels.EventObserver
import com.example.volumeprofiler.viewmodels.MapsCoordinatesViewModel
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.maps.model.LatLng
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

    private var currentScene: Byte = 0

    private var _mainBinding: MapsSelectLocationFragmentBinding? = null
    private val mainBinding: MapsSelectLocationFragmentBinding get() = _mainBinding!!

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()
    private val viewModel: MapsCoordinatesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            this.currentScene = savedInstanceState.getByte(KEY_CURRENT_SCENE, 0)
        }
        setTransitions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putByte(KEY_CURRENT_SCENE, currentScene)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = requireActivity() as Callback
    }

    override fun onDetach() {
        parentActivity = null
        super.onDetach()
    }

    override fun onDestroyView() {
        _mainBinding = null
        super.onDestroyView()
    }

    private fun setTransitions(): Unit {
        val inflater = TransitionInflater.from(requireContext())
        this.exitTransition = Scale()
        this.enterTransition = inflater.inflateTransition(R.transition.slide_left)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _mainBinding = MapsSelectLocationFragmentBinding.inflate(inflater, container, false)
        sharedViewModel.addressLine.observe(viewLifecycleOwner, EventObserver<String>{
            mainBinding.addressEditText.setText(it)
        })
        sharedViewModel.latLng.observe(viewLifecycleOwner, EventObserver<LatLng> {
            mainBinding.latitudeTextInput.setText(it.latitude.toString())
            mainBinding.longitudeEditText.setText(it.longitude.toString())
        })
        viewModel.observableLatitudeEditStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            mainBinding.latitudeTextInputLayout.error = if (it) null else "Invalid input"
        })
        viewModel.observableLongitudeEditStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            mainBinding.longitudeTextInputLayout.error = if (it) null else "Invalid input"
        })
        viewModel.observableAddressEditState.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            mainBinding.addressTextInputLayout.error = if (it) null else "Invalid input"
        })
        viewModel.observableMetrics.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            updateMetrics(it)
        })
        return mainBinding.root
    }

    private fun addTextObservers(): Unit {
        mainBinding.latitudeTextInput.addTextChangedListener(getTextWatcher(mainBinding.latitudeTextInputLayout))
        mainBinding.longitudeEditText.addTextChangedListener(getTextWatcher(mainBinding.longitudeTextInputLayout))
        mainBinding.addressEditText.addTextChangedListener(getTextWatcher(mainBinding.addressTextInputLayout))
    }

    private fun setTextFilters(): Unit {
        val coordinatesInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend -> TextUtil.filterCoordinatesInput(source) }
        val streetAddressInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->  TextUtil.filterStreetAddressInput(source) }
        val coordinateFilters: Array<InputFilter> = arrayOf(coordinatesInputFilter)
        mainBinding.latitudeTextInput.filters = coordinateFilters
        mainBinding.longitudeEditText.filters = coordinateFilters
        mainBinding.addressEditText.filters = arrayOf(streetAddressInputFilter)
    }

    private fun getTextWatcher(editTextLayout: TextInputLayout?): TextWatcher {
        return object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                when (editTextLayout?.id) {

                    R.id.addressTextInputLayout -> {
                        viewModel.validateAddressInput(s)
                    }
                    R.id.longitudeTextInputLayout -> {
                        viewModel.validateLongitudeInput(s)
                    }
                    R.id.latitudeTextInputLayout -> {
                        viewModel.validateLatitudeInput(s)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private suspend fun getCoordinatesFromAddress(): LatLng? {
        return if (mainBinding.addressEditText.text!!.isNotEmpty()) {
            val gc: Geocoder = Geocoder(requireContext(), Locale.getDefault())
            withContext(Dispatchers.IO) {
                val addressList: List<Address>? = gc.getFromLocationName(mainBinding.addressEditText.text.toString(), 1)
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
                sharedViewModel.setLatLng(latLng)
            } else {
                mainBinding.addressTextInputLayout.error = "Could not reverse specified address"
                AnimUtil.shakeAnimation(mainBinding.addressTextInputLayout)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainBinding.addressImage.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                mainBinding.addressImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val parentFragmentView: View = requireParentFragment().requireView()
                parentActivity?.setHalfExpandedRatio(ViewUtil.calculateHalfExpandedRatio(
                    parentFragmentView.findViewById(R.id.bottomSheetRoot),
                    mainBinding.addressImage))
            }
        })
        mainBinding.metricsSpinner.adapter = ArrayAdapter<Metrics>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(Metrics.METERS, Metrics.KILOMETERS))
        mainBinding.metricsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setMetrics(parent!!.selectedItem as Metrics)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        setTextFilters()
        addTextObservers()
        val onClickListener: View.OnClickListener = View.OnClickListener {
            when (it.id) {
                mainBinding.setLocationButton.id -> {
                    var isInputValid: Boolean = true
                    if (viewModel.observableLatitudeEditStatus.value == false) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(mainBinding.latitudeTextInputLayout)
                    }
                    if (viewModel.observableLongitudeEditStatus.value == false) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(mainBinding.longitudeTextInputLayout)
                    }
                    if (isInputValid) {
                        val latLng: LatLng = LatLng(mainBinding.latitudeTextInput.text.toString().toDouble(),
                                mainBinding.longitudeEditText.text.toString().toDouble())
                        sharedViewModel.setLatLng(latLng)
                    }
                }
                mainBinding.setAddressButton.id -> {
                    reverseAddress()
                }
            }
        }
        mainBinding.setLocationButton.setOnClickListener(onClickListener)
        mainBinding.setAddressButton.setOnClickListener(onClickListener)
        setSliderOnChangeListener()
    }

    private fun updateMetrics(metrics: Metrics): Unit {
        if (metrics == Metrics.KILOMETERS) {
            val value = sharedViewModel.getRadius()!! / 1000
            mainBinding.radiusSlider.valueTo = Metrics.KILOMETERS.sliderMaxValue
            mainBinding.radiusSlider.valueFrom = Metrics.KILOMETERS.sliderMinValue
            mainBinding.radiusSlider.value = value
        } else {
            val value: Float = if (sharedViewModel.getRadius()!! > Metrics.METERS.sliderMaxValue) {
                Metrics.METERS.sliderMaxValue
            } else {
                sharedViewModel.getRadius()!!
            }
            mainBinding.radiusSlider.valueTo = Metrics.METERS.sliderMaxValue
            mainBinding.radiusSlider.valueFrom = Metrics.METERS.sliderMinValue
            mainBinding.radiusSlider.value = value
        }
    }

    private fun onSliderProgressChanged(progress: Float): Unit {
        if (viewModel.observableMetrics.value == Metrics.METERS) {
            sharedViewModel.setRadius(progress)
        }
        else {
            sharedViewModel.setRadius(progress * 1000)
        }
    }

    private fun setSliderOnChangeListener(): Unit {
        mainBinding.radiusSlider.addOnChangeListener { slider, value, fromUser ->
            onSliderProgressChanged(value)
        }
    }

    companion object {
        private const val KEY_CURRENT_SCENE: String = "key_current_scene"
        private const val COORDINATES_EDIT_TEXT_ERROR: String = "Enter a correct value"
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            v?.clearFocus()
            return true
        }
        return false
    }
}