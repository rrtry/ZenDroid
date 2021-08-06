package com.example.volumeprofiler.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.AnimUtils
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsCoordinatesFragment: Fragment() {

    private lateinit var latitudeTextInputLayout: TextInputLayout
    private lateinit var longitudeTextInputLayout: TextInputLayout
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var addressTextInputLayout: TextInputLayout

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return inflater.inflate(R.layout.maps_select_location, container, false)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val radiusSeekBar: SeekBar = view.findViewById(R.id.radiusSeekBar)
        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.i("MapsActivity", "onProgressChanged: $progress")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) { }

        })
        val setLocationButton: Button = view.findViewById(R.id.setLocationButton)
        val addressButton: Button = view.findViewById(R.id.setAddressButton)
        addressTextInputLayout = view.findViewById(R.id.addressTextInputLayout)

        latitudeEditText = view.findViewById(R.id.latitudeTextInput)
        longitudeEditText = view.findViewById(R.id.longitudeTextInput)
        latitudeTextInputLayout = view.findViewById(R.id.latitudeTextInputLayout)
        longitudeTextInputLayout = view.findViewById(R.id.longitudeTextInputLayout)
        addressEditText = view.findViewById(R.id.addressTextInput)

        latitudeTextInputLayout.boxStrokeErrorColor = ColorStateList.valueOf(Color.RED)
        longitudeTextInputLayout.boxStrokeErrorColor = ColorStateList.valueOf(Color.RED)

        val coordinatesInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend -> filterCoordinatesInput(source) }
        val streetAddressInputFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->  filterStreetAddressInput(source) }
        val coordinateFilters: Array<InputFilter> = arrayOf(coordinatesInputFilter)
        val streetAddressFilters: Array<InputFilter> = arrayOf(streetAddressInputFilter)
        latitudeEditText.filters = coordinateFilters
        longitudeEditText.filters = coordinateFilters
        addressEditText.filters = streetAddressFilters

        addTextObservers()

        addressButton.setOnClickListener {
            lifecycleScope.launch {
                val latLng: LatLng? = getCoordinatesFromAddress()
                if (latLng != null) {
                    sharedViewModel.animateCameraMovement = false
                    sharedViewModel.latLng.value = latLng
                } else {
                    addressTextInputLayout.error = "Could not reverse specified address"
                    AnimUtils.shakeAnimation(addressTextInputLayout)
                }
            }
        }

        setLocationButton.setOnClickListener {
            var isInputValid: Boolean = true
            if (!validateCoordinatesInput(latitudeEditText.text)) {
                isInputValid = false
                AnimUtils.shakeAnimation(latitudeTextInputLayout)
            }
            if (!validateCoordinatesInput(longitudeEditText.text)) {
                isInputValid = false
                AnimUtils.shakeAnimation(longitudeTextInputLayout)
            }
            if (isInputValid) {
                val latLng: LatLng = LatLng(latitudeEditText.text.toString().toDouble(),
                        longitudeEditText.text.toString().toDouble())
                sharedViewModel.animateCameraMovement = false
                sharedViewModel.latLng.value = latLng
            }
        }
    }

    companion object {

        private const val KEY_ADDRESS: String = "key_string"
        private const val KEY_LATITUDE: String = "key_latitude"
        private const val KEY_LONGITUDE: String = "key_longitude"
        private const val ERROR_TEXT: String = "Enter a correct value"
        private const val LOG_TAG: String = "MapsCoordinatesFragment"
    }
}