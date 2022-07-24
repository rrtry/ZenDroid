package ru.rrtry.silentdroid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.rrtry.silentdroid.databinding.ProfileImageViewBinding
import ru.rrtry.silentdroid.interfaces.IconSelectedListener

class DrawableAdapter(
    private val drawableList: List<Int>,
    private val listener: IconSelectedListener
    ): RecyclerView.Adapter<DrawableAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ProfileImageViewBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(drawableRes: Int) {
            binding.profileImage.setImageDrawable(
                ContextCompat.getDrawable(binding.root.context, drawableRes)
            )
            binding.profileImage.setOnClickListener {
                listener.onDrawableSelected(drawableRes)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ProfileImageViewBinding.inflate(
                LayoutInflater.from(parent.context)
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(drawableList[position])
    }

    override fun getItemCount(): Int {
        return drawableList.size
    }
}