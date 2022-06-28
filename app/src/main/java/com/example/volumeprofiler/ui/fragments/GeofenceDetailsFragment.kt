package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.*
import com.example.volumeprofiler.databinding.MapsSelectLocationFragmentBinding
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.MapsUtil
import kotlinx.coroutines.launch

class GeofenceDetailsFragment: ViewBindingFragment<MapsSelectLocationFragmentBinding>() {

    private val sharedViewModel: GeofenceSharedViewModel by activityViewModels()
    private var currentMetrics: Metrics = Metrics.METERS

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): MapsSelectLocationFragmentBinding {
        return MapsSelectLocationFragmentBinding.inflate(inflater, container, false)
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
        savedInstanceState?.let { bundle ->
            currentMetrics = bundle.getSerializable(EXTRA_CURRENT_METRICS) as Metrics
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewBinding.latitudeTextInput.text.isNullOrEmpty()) {
            viewBinding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
        }
        if (viewBinding.longitudeEditText.text.isNullOrEmpty()) {
            viewBinding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
        }
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
        super.onCreateView(inflater, container, savedInstanceState)
        viewBinding.latitudeTextInput.doAfterTextChanged {
            it?.let {
                if (MapsUtil.isLatitude(it.toString())) {
                    viewBinding.latitudeTextInputLayout.error = null
                } else {
                    viewBinding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
                }
            }
        }
        viewBinding.longitudeEditText.doAfterTextChanged {
            it?.let {
                if (MapsUtil.isLongitude(it.toString())) {
                    viewBinding.longitudeTextInputLayout.error = null
                } else {
                    viewBinding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
                }
            }
        }
        viewBinding.metricsSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(Metrics.METERS, Metrics.KILOMETERS)
        )
        viewBinding.metricsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

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
        viewBinding.radiusSlider.addOnChangeListener { _, value, _ ->
            if (currentMetrics == Metrics.METERS) {
                sharedViewModel.setRadius(value)
            } else {
                sharedViewModel.setRadius(value * 1000)
            }
        }
        viewBinding.setLocationButton.setOnClickListener { view ->

            var valid: Boolean = true

            if (!MapsUtil.isLatitude(viewBinding.latitudeTextInput.text.toString())) {
                valid = false
                Animations.shake(viewBinding.latitudeTextInputLayout)
                viewBinding.latitudeTextInputLayout.error = getString(R.string.latitude_text_input_error)
            }
            if (!MapsUtil.isLongitude(viewBinding.longitudeEditText.text.toString())) {
                valid = false
                Animations.shake(viewBinding.longitudeTextInputLayout)
                viewBinding.longitudeTextInputLayout.error = getString(R.string.longitude_text_input_error)
            }
            if (valid) {
                sharedViewModel.setLatLng(
                    LatLng(
                        viewBinding.latitudeTextInput.text.toString().toDouble(),
                        viewBinding.longitudeEditText.text.toString().toDouble()
                    )
                )
            }
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.title.collect { title ->
                        title?.also { viewBinding.titleEditText.setText(it) }
                    }
                }
                launch {
                    sharedViewModel.latLng.collect { latLng ->
                        latLng?.also {
                            viewBinding.latitudeTextInput.setText(it.first.latitude.toString())
                            viewBinding.longitudeEditText.setText(it.first.longitude.toString())
                        }
                    }
                }
            }
        }
    }

    private fun updateMetrics(metrics: Metrics) {

        currentMetrics = metrics

        viewBinding.radiusSlider.valueTo = metrics.max
        viewBinding.radiusSlider.valueFrom = metrics.min

        if (metrics == Metrics.KILOMETERS) {
            viewBinding.radiusSlider.value = sharedViewModel.getRadius() / 1000
        } else {
            viewBinding.radiusSlider.value = if (sharedViewModel.getRadius() > Metrics.METERS.max) {
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