package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import java.util.*

class MapsProfileSelectionFragment: Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.maps_select_profiles_fragment, container, false)
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