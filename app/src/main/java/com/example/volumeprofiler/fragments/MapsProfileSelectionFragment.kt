package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.TransitionInflater
import com.example.volumeprofiler.R
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.viewmodels.MapsProfileViewModel
import java.util.*

class MapsProfileSelectionFragment: Fragment() {

    private val myViewModel: MapsProfileViewModel by viewModels()

    private lateinit var onEnterSpinner: Spinner
    private lateinit var onExitSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions()
    }

    private fun setTransitions(): Unit {
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.slide_right)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.maps_select_profiles_fragment, container, false)
        onEnterSpinner = view.findViewById(R.id.onEnterSpinner)
        onExitSpinner = view.findViewById(R.id.onExitSpinner)
        myViewModel.profiles.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val arrayAdapter: ArrayAdapter<Profile> = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, it)
            onEnterSpinner.apply {
                this.adapter = arrayAdapter
                this.setSelection(if (getArgs() == null) 0 else getAdapterPosition(it, getArgs()!!.first))
            }
            onExitSpinner.apply {
                this.adapter = arrayAdapter
                this.setSelection(if (getArgs() == null) 0 else getAdapterPosition(it, getArgs()!!.second))
            }
        })
        return view
    }

    private fun getArgs(): Pair<UUID, UUID>? {
        return arguments?.getSerializable(ID_PAIR) as? Pair<UUID, UUID>
    }

    private fun getAdapterPosition(list: List<Profile>, id: UUID): Int {
        for ((index, i) in list.withIndex()) {
            if (i.id == id) {
                return index
            }
        }
        return 0
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