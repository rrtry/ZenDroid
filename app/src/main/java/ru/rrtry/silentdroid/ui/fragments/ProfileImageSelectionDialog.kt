package ru.rrtry.silentdroid.ui.fragments

import android.app.Dialog
import android.content.res.TypedArray
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.adapters.DrawableAdapter
import ru.rrtry.silentdroid.interfaces.IconSelectedListener
import ru.rrtry.silentdroid.ui.GridSpacingDecoration
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import ru.rrtry.silentdroid.databinding.ImageSelectionDialogLayoutBinding

@AndroidEntryPoint
class ProfileImageSelectionDialog: DialogFragment(), IconSelectedListener {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    private var bindingImpl: ImageSelectionDialogLayoutBinding? = null
    private val binding: ImageSelectionDialogLayoutBinding
    get() = bindingImpl!!

    override fun onDrawableSelected(res: Int) {
        viewModel.iconRes.value = res
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        AlertDialog.Builder(requireContext()).apply {
            setView(layoutInflater.inflate(R.layout.image_selection_dialog_layout, null))
            return show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        bindingImpl = ImageSelectionDialogLayoutBinding.inflate(layoutInflater, container, false)
        binding.recyclerView.adapter = DrawableAdapter(getDrawablesFromResources(), this)
        binding.recyclerView.layoutManager = GridLayoutManager(
            requireContext(), 4, GridLayoutManager.VERTICAL, false
        )
        binding.recyclerView.addItemDecoration(
            GridSpacingDecoration(4, 50, true)
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }

    private fun getDrawablesFromResources(): List<Int> {

        val typedArray: TypedArray = resources.obtainTypedArray(R.array.profile_icons)
        val drawables: ArrayList<Int> = arrayListOf()

        (0 until typedArray.length()).forEach { res ->
            drawables.add(typedArray.getResourceId(res, -1))
        }
        typedArray.recycle()
        return drawables
    }
}