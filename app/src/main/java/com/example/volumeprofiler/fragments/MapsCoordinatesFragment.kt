package com.example.volumeprofiler.fragments

import android.content.Context
import android.os.Bundle
import android.text.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.MapsSelectLocationFragmentBinding
import com.example.volumeprofiler.util.GeocoderUtil
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.MapsCoordinatesViewModel
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MapsCoordinatesFragment: Fragment(), TextView.OnEditorActionListener {

    private var parentActivity: Callback? = null

    interface Callback {

        fun setHalfExpandedRatio(ratio: Float): Unit

        fun getPeekHeight(): Int
    }

    @Inject lateinit var geocoderUtil: GeocoderUtil

    private var _binding: MapsSelectLocationFragmentBinding? = null
    private val binding: MapsSelectLocationFragmentBinding get() = _binding!!

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()
    private val viewModel: MapsCoordinatesViewModel by viewModels()

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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setTransitions(): Unit {
        val exitTransition: Transition = Slide(Gravity.START)
        val enterTransition: Transition = Slide(Gravity.START)

        enterTransition.startDelay = 200
        this.exitTransition = exitTransition
        this.enterTransition = enterTransition
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = MapsSelectLocationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun addTextObservers(): Unit {
        binding.latitudeTextInput.addTextChangedListener(getTextWatcher(binding.latitudeTextInputLayout))
        binding.longitudeEditText.addTextChangedListener(getTextWatcher(binding.longitudeTextInputLayout))
    }

    private fun setTextFilters(): Unit {
        val coordinatesInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend -> TextUtil.filterCoordinatesInput(source) }
        val coordinateFilters: Array<InputFilter> = arrayOf(coordinatesInputFilter)
        binding.latitudeTextInput.filters = coordinateFilters
        binding.longitudeEditText.filters = coordinateFilters
    }

    private fun getTextWatcher(editTextLayout: TextInputLayout?): TextWatcher {
        return object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                when (editTextLayout?.id) {

                    R.id.longitudeTextInputLayout -> {
                        viewModel.validateLongitudeInput(s)
                    }
                    R.id.latitudeTextInputLayout -> {
                        viewModel.validateLatitudeInput(s)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.latLng.collect {
                        binding.latitudeTextInput.setText(it?.latitude.toString())
                        binding.longitudeEditText.setText(it?.longitude.toString())
                    }
                }
                launch {
                    viewModel.latitudeEditStatus.collect {
                        binding.latitudeTextInputLayout.error = if (it) null else "Invalid input"
                    }
                }
                launch {
                    viewModel.longitudeEditStatus.collect {
                        binding.longitudeTextInputLayout.error = if (it) null else "Invalid input"
                    }
                }
                launch {
                    viewModel.metrics.collect {
                        updateMetrics(it)
                    }
                }
            }
        }
        binding.coordinatesTitle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                binding.coordinatesTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val parentFragmentView: View = requireParentFragment().requireView()
                parentActivity?.setHalfExpandedRatio(ViewUtil.calculateHalfExpandedRatio(
                    parentFragmentView.findViewById(R.id.bottomSheetRoot),
                    binding.coordinatesTitle))
            }
        })
        binding.metricsSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(Metrics.METERS, Metrics.KILOMETERS))
        binding.metricsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                binding.setLocationButton.id -> {
                    var isInputValid: Boolean = true
                    if (!viewModel.latitudeEditStatus.value) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(binding.latitudeTextInputLayout)
                    }
                    if (!viewModel.longitudeEditStatus.value) {
                        isInputValid = false
                        AnimUtil.shakeAnimation(binding.longitudeTextInputLayout)
                    }
                    if (isInputValid) {
                        val latLng = LatLng(binding.latitudeTextInput.text.toString().toDouble(),
                                binding.longitudeEditText.text.toString().toDouble())
                        sharedViewModel.setLatLng(latLng)
                    }
                }
            }
        }
        binding.setLocationButton.setOnClickListener(onClickListener)
        setSliderOnChangeListener()
    }

    private fun updateMetrics(metrics: Metrics): Unit {
        if (metrics == Metrics.KILOMETERS) {
            val value = sharedViewModel.getRadius() / 1000
            binding.radiusSlider.valueTo = Metrics.KILOMETERS.sliderMaxValue
            binding.radiusSlider.valueFrom = Metrics.KILOMETERS.sliderMinValue
            binding.radiusSlider.value = value
        } else {
            val value: Float = if (sharedViewModel.getRadius() > Metrics.METERS.sliderMaxValue) {
                Metrics.METERS.sliderMaxValue
            } else {
                sharedViewModel.getRadius()
            }
            binding.radiusSlider.valueTo = Metrics.METERS.sliderMaxValue
            binding.radiusSlider.valueFrom = Metrics.METERS.sliderMinValue
            binding.radiusSlider.value = value
        }
    }

    private fun onSliderProgressChanged(progress: Float): Unit {
        if (viewModel.metrics.value == Metrics.METERS) {
            sharedViewModel.setRadius(progress)
        }
        else {
            sharedViewModel.setRadius(progress * 1000)
        }
    }

    private fun setSliderOnChangeListener(): Unit {
        binding.radiusSlider.addOnChangeListener { slider, value, fromUser ->
            onSliderProgressChanged(value)
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            v?.clearFocus()
            return true
        }
        return false
    }
}