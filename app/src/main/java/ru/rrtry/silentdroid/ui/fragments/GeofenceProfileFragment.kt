package ru.rrtry.silentdroid.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import ru.rrtry.silentdroid.databinding.MapsSelectProfilesFragmentBinding
import ru.rrtry.silentdroid.viewmodels.GeofenceSharedViewModel

class GeofenceProfileFragment: ViewBindingFragment<MapsSelectProfilesFragmentBinding>() {

    private val viewModel: GeofenceSharedViewModel by activityViewModels()

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): MapsSelectProfilesFragmentBinding {
        return MapsSelectProfilesFragmentBinding.inflate(layoutInflater)
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
        super.onCreateView(inflater, container, savedInstanceState)
        viewBinding.viewModel = viewModel
        viewBinding.lifecycleOwner = viewLifecycleOwner
        return viewBinding.root
    }
}