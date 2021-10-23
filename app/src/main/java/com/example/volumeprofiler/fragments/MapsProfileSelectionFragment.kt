package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.Slide
import androidx.transition.Transition
import com.example.volumeprofiler.databinding.MapsSelectProfilesFragmentBinding
import com.example.volumeprofiler.viewmodels.MapsSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MapsProfileSelectionFragment: Fragment() {

    private var _binding: MapsSelectProfilesFragmentBinding? = null
    private val binding: MapsSelectProfilesFragmentBinding get() = _binding!!

    private val viewModel: MapsSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions()
    }

    private fun setTransitions(): Unit {
        val enterTransition: Transition = Slide(Gravity.END)
        val exitTransition: Transition = Slide(Gravity.END)
        enterTransition.startDelay = 200
        this.enterTransition = enterTransition
        this.exitTransition = exitTransition
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

    companion object {

        const val ID_PAIR: String = "pair"

        fun buildArgs(ids: Pair<UUID, UUID>?): Fragment {
            val bundle: Bundle = Bundle().apply {
                if (ids != null) {
                    this.putSerializable(ID_PAIR, ids)
                }
            }
            return MapsProfileSelectionFragment().apply {
                this.arguments = bundle
            }
        }
    }
}