package com.example.volumeprofiler.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.MapsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LocationsListFragment: Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.locations_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent: Intent = Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }
    }
}