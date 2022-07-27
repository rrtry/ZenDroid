package ru.rrtry.silentdroid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import ru.rrtry.silentdroid.databinding.SpinnerProfileViewBinding
import ru.rrtry.silentdroid.entities.Profile

class ProfileSpinnerAdapter(
    context: Context,
    layoutRes: Int,
    private val profiles: List<Profile>
): ArrayAdapter<Profile>(context, layoutRes) {

    override fun getCount() = profiles.size
    override fun getItem(position: Int) = profiles[position]
    override fun getItemId(position: Int) = position.toLong()

    private fun inflateView(context: Context, position: Int): View {
        val binding = SpinnerProfileViewBinding.inflate(
            LayoutInflater.from(context)
        )
        profiles[position].let { profile ->
            binding.profileImage.setImageDrawable(ContextCompat.getDrawable(
                context, profile.iconRes
            ))
            binding.profileTitle.text = profile.title
        }
        return binding.root
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return inflateView(context, position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return inflateView(context, position)
    }
}