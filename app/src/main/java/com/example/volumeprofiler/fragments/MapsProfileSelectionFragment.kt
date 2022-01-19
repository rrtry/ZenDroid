package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionSet
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.MapsSelectProfilesFragmentBinding
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.*

@AndroidEntryPoint
class MapsProfileSelectionFragment: Fragment() {

    private var _binding: MapsSelectProfilesFragmentBinding? = null
    private val binding: MapsSelectProfilesFragmentBinding get() = _binding!!

    private val viewModel: MapsSharedViewModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions()
    }

    private fun setTransitions(): Unit {
        val transitionSet: TransitionSet = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade())
            addTransition(Slide(Gravity.END))
        }

        enterTransition = transitionSet
        exitTransition = transitionSet
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