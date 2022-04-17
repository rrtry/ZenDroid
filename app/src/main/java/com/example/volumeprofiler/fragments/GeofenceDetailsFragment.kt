package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.*
import com.example.volumeprofiler.databinding.MapsSelectLocationFragmentBinding
import com.example.volumeprofiler.util.GeoUtil
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.volumeprofiler.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GeofenceDetailsFragment: Fragment() {

    private var _binding: MapsSelectLocationFragmentBinding? = null
    private val binding: MapsSelectLocationFragmentBinding get() = _binding!!

    private val sharedViewModel: GeofenceSharedViewModel by activityViewModels()

    private var currentMetrics: Metrics = Metrics.METERS

    private val latitudeTextWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let {
                if (GeoUtil.isLatitude(s.toString())) {
                    binding.latitudeTextInputLayout.error = null
                } else {
                    binding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
                }
                return
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private val longitudeTextWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let {
                if (GeoUtil.isLongitude(s.toString())) {
                    binding.longitudeTextInputLayout.error = null
                } else {
                    binding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
                }
                return
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER

            addTransition(Fade())
            addTransition(Slide(Gravity.START))

            enterTransition = this
            exitTransition = this
        }
        savedInstanceState?.let {
            currentMetrics = it.getSerializable(EXTRA_CURRENT_METRICS) as Metrics
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.latitudeTextInput.text.isNullOrEmpty()) {
            binding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
        }
        if (binding.longitudeEditText.text.isNullOrEmpty()) {
            binding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_CURRENT_METRICS, currentMetrics)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MapsSelectLocationFragmentBinding.inflate(inflater, container, false)

        binding.latitudeTextInput.addTextChangedListener(latitudeTextWatcher)
        binding.longitudeEditText.addTextChangedListener(longitudeTextWatcher)

        binding.metricsSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(Metrics.METERS, Metrics.KILOMETERS)
        )
        binding.metricsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    parent?.selectedItem.also {
                        updateMetrics(it as Metrics)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        binding.radiusSlider.addOnChangeListener { slider, value, fromUser ->
            if (currentMetrics == Metrics.METERS) {
                sharedViewModel.setRadius(value)
            } else {
                sharedViewModel.setRadius(value * 1000)
            }
        }
        binding.setLocationButton.setOnClickListener { view ->

            var valid = true

            if (!GeoUtil.isLatitude(binding.latitudeTextInput.text.toString())) {
                valid = false
                AnimUtil.shake(binding.latitudeTextInputLayout)
                binding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
            }
            if (!GeoUtil.isLongitude(binding.longitudeEditText.text.toString())) {
                valid = false
                AnimUtil.shake(binding.longitudeTextInputLayout)
                binding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
            }
            if (valid) {
                sharedViewModel.setLatLng(
                    LatLng(
                        binding.latitudeTextInput.text.toString().toDouble(),
                        binding.longitudeEditText.text.toString().toDouble()
                    )
                )
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.title.collect { title ->
                        title?.also {
                            binding.titleEditText.setText(it)
                        }
                    }
                }
                launch {
                    sharedViewModel.latLng.collect { latLng ->
                        latLng?.also {
                            binding.latitudeTextInput.setText(it.first.latitude.toString())
                            binding.longitudeEditText.setText(it.first.longitude.toString())
                        }
                    }
                }
            }
        }
    }

    private fun updateMetrics(metrics: Metrics) {

        currentMetrics = metrics

        binding.radiusSlider.valueTo = metrics.max
        binding.radiusSlider.valueFrom = metrics.min

        if (metrics == Metrics.KILOMETERS) {
            binding.radiusSlider.value = sharedViewModel.getRadius() / 1000
        } else {
            binding.radiusSlider.value = if (sharedViewModel.getRadius() > Metrics.METERS.max) {
                Metrics.METERS.max
            } else {
                sharedViewModel.getRadius()
            }
        }
    }

    companion object {

        private const val EXTRA_CURRENT_METRICS: String = "extra_current_metrics"
    }
}