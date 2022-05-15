package com.example.volumeprofiler.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.MapDecorationItemBinding
import com.example.volumeprofiler.entities.MapType
import java.lang.ref.WeakReference

class MapTypeAdapter(
    private val listener: WeakReference<Callback>,
    private val types: List<MapType>
    ): RecyclerView.Adapter<MapTypeAdapter.TypeHolder>() {

    interface Callback {

        fun onTypeSelected(type: Int)
    }

    inner class TypeHolder(private val binding: MapDecorationItemBinding):
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {

        fun bind(item: MapType) {
            binding.preview.setImageBitmap(item.preview)
            binding.overlayName.text = item.title
        }

        override fun onClick(v: View?) {
            listener.get()?.onTypeSelected(
                types[bindingAdapterPosition].type
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeHolder {
        return TypeHolder(
            MapDecorationItemBinding.inflate(
                LayoutInflater.from(parent.context)
            )
        )
    }

    override fun onBindViewHolder(holder: TypeHolder, position: Int) {
        holder.bind(types[position])
    }

    override fun getItemCount(): Int {
        return types.size
    }
}