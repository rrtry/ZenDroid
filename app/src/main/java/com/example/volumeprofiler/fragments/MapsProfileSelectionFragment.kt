package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.transition.TransitionInflater
import com.example.volumeprofiler.R
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel

class MapsProfileSelectionFragment: Fragment() {

    private val sharedViewModel: MapsSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions()
    }

    private fun setTransitions(): Unit {
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_from_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.maps_select_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arrayAdapter: ArrayAdapter<Profile> = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item)
        val onExitSpinner: Spinner = view.findViewById(R.id.onExitSpinner)
        val onEnterSpinner: Spinner = view.findViewById(R.id.onEnterSpinner)
        onExitSpinner.adapter = arrayAdapter
        onEnterSpinner.adapter = arrayAdapter
        sharedViewModel.profileListLiveData.observe(viewLifecycleOwner, object : Observer<List<Profile>> {

            override fun onChanged(t: List<Profile>?) {
                if (t != null) {
                    for (i in t) {
                        arrayAdapter.add(i)
                    }
                    if (sharedViewModel.onEnterProfile != null) {
                        onEnterSpinner.setSelection(arrayAdapter.getPosition(sharedViewModel.onEnterProfile))
                    }
                    if (sharedViewModel.onExitProfile != null) {
                        onExitSpinner.setSelection(arrayAdapter.getPosition(sharedViewModel.onExitProfile))
                    }
                }
            }
        })
        onEnterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedViewModel.onEnterProfile = parent?.getItemAtPosition(position) as Profile
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                sharedViewModel.onEnterProfile = null
            }
        }
        onExitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedViewModel.onExitProfile = parent?.getItemAtPosition(position) as Profile
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                sharedViewModel.onExitProfile = null
            }
        }
    }
}