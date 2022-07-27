package ru.rrtry.silentdroid.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import ru.rrtry.silentdroid.adapters.StyleAdapter
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.MapStyle
import ru.rrtry.silentdroid.util.ViewUtil.Companion.getDrawable
import ru.rrtry.silentdroid.viewmodels.GeofenceSharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import ru.rrtry.silentdroid.databinding.MapStyleDialogBinding
import java.lang.ref.WeakReference

@AndroidEntryPoint
class MapThemeSelectionDialog:
    BottomSheetDialogFragment(),
    StyleAdapter.Callback {

    private val viewModel: GeofenceSharedViewModel by activityViewModels()

    private var bindingImpl: MapStyleDialogBinding? = null
    private val binding: MapStyleDialogBinding get() = bindingImpl!!

    override fun onStyleSelected(style: Int) {
        viewModel.onMapStyleChanged(style)
    }

    private fun getMapStyles(): List<MapStyle> {
        return listOf(
            MapStyle("Default", R.raw.mapstyle_standart, getDrawable(R.drawable.mapstyle_default)),
            MapStyle("Silver", R.raw.mapstyle_silver, getDrawable(R.drawable.mapstyle_silver)),
            MapStyle("Retro", R.raw.mapstyle_retro, getDrawable(R.drawable.mapstyle_retro)),
            MapStyle("Dark", R.raw.mapstyle_dark, getDrawable(R.drawable.mapstyle_dark)),
            MapStyle("Night", R.raw.mapstyle_night, getDrawable(R.drawable.mapstyle_night)),
            MapStyle("Aubergine", R.raw.mapstyle_aubergine, getDrawable(R.drawable.mapstyle_aubergine))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingImpl = MapStyleDialogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapStyleRecyclerView.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        binding.mapStyleRecyclerView.adapter = StyleAdapter(WeakReference(this), getMapStyles())
    }
}