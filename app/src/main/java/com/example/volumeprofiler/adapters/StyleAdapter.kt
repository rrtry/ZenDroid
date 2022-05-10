package com.example.volumeprofiler.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntegerRes
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.MapDecorationItemBinding
import com.example.volumeprofiler.entities.MapStyle
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.EdgeTreatment
import java.lang.ref.WeakReference

class StyleAdapter(
    listenerRef: WeakReference<Callback>,
    private val styles: List<MapStyle>
    ): RecyclerView.Adapter<StyleAdapter.StyleHolder>() {

    private val listener: Callback = listenerRef.get()!!

    interface Callback {

        fun onStyleSelected(@IntegerRes style: Int)
    }

    inner class StyleHolder(private val binding: MapDecorationItemBinding):
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(item: MapStyle) {
            binding.preview.clipToOutline = true
            binding.preview.setImageDrawable(item.preview)
            binding.overlayName.text = item.title
        }

        override fun onClick(v: View?) {
            listener.onStyleSelected(styles[bindingAdapterPosition].resId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleHolder {
        return StyleHolder(
            MapDecorationItemBinding.inflate(LayoutInflater.from(parent.context))
        )
    }

    override fun onBindViewHolder(holder: StyleHolder, position: Int) {
        holder.bind(styles[position])
    }

    override fun getItemCount(): Int {
        return styles.size
    }
}