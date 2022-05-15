package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.example.volumeprofiler.databinding.MapsSelectProfilesFragmentBinding
import com.example.volumeprofiler.viewmodels.GeofenceSharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeofenceProfileFragment: Fragment() {

    private var _binding: MapsSelectProfilesFragmentBinding? = null
    private val binding: MapsSelectProfilesFragmentBinding get() = _binding!!

    private val viewModel: GeofenceSharedViewModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TransitionSet().apply {

            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade())
            addTransition(Slide(Gravity.END))

            enterTransition = this
            exitTransition = this
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = MapsSelectProfilesFragmentBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }
}