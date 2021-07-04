package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOffVisualRestrictions
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScreenOnVisualRestrictions
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import android.util.Log

class CustomPolicyFragment: Fragment(), VisualRestrictionsCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.custom_restrictions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
    }

    private fun initViews(view: View): Unit {
        val screenOnLayout: LinearLayout = view.findViewById(R.id.whenScreenIsOn)
        val screenOffLayout: LinearLayout = view.findViewById(R.id.whenScreenIsOff)
        val screenOnDescription: TextView = view.findViewById(R.id.whenScreenIsOnDescription)
        val screenOffDescription: TextView = view.findViewById(R.id.whenScreenIsOffDescription)

        val onClickListener = View.OnClickListener {
            val fragment: DialogFragment =
                    if (it.id == screenOnLayout.id)
                        ScreenOnVisualRestrictions.newInstance(viewModel.mutableProfile!!)
                    else
                        ScreenOffVisualRestrictions.newInstance(viewModel.mutableProfile!!)
            fragment.apply {
                this.setTargetFragment(this@CustomPolicyFragment, REQUEST_CODE)
            }
            fragment.show(requireActivity().supportFragmentManager, null)
        }
        screenOnLayout.setOnClickListener(onClickListener)
        screenOffLayout.setOnClickListener(onClickListener)
    }

    companion object {
        private const val REQUEST_CODE: Int = 3
    }

    override fun onPolicySelected(effects: String) {
        Log.i("CustomPolicyFragment", effects)
    }
}