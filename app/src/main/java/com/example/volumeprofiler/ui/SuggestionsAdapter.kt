package com.example.volumeprofiler.ui

import android.app.SearchManager.SUGGEST_COLUMN_TEXT_1
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.cursoradapter.widget.CursorAdapter
import com.example.volumeprofiler.databinding.LocationRecentQueryItemViewBinding
import com.example.volumeprofiler.databinding.LocationSuggestionItemViewBinding
import com.example.volumeprofiler.entities.LocationSuggestion
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_ICON
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LATITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_LONGITUDE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.COLUMN_VIEW_TYPE
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_CONNECTIVITY_ERROR
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_EMPTY_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_LOCATION_RECENT_QUERY
import com.example.volumeprofiler.util.SuggestionQueryColumns.Companion.VIEW_TYPE_LOCATION_SUGGESTION

@Suppress("Range")
class SuggestionsAdapter(private val context: Context, cursor: MatrixCursor?):
    CursorAdapter(context, cursor, 0) {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val listener: Callback
    get() = context as Callback

    interface Callback {

        fun onSuggestionClick(suggestion: LocationSuggestion, itemViewType: Int)
        fun onRemoveRecentQuery(locationSuggestion: LocationSuggestion)
    }

    private class ViewHolder {

        lateinit var suggestionIcon: ImageView
        lateinit var suggestionText: TextView
        var removeRecentQueryButton: ImageButton? = null
        var itemViewType: Int = IGNORE_ITEM_VIEW_TYPE
    }

    override fun getItem(position: Int): Cursor {
        return super.getItem(position) as Cursor
    }

    private fun getItemViewType(cursor: Cursor?): Int {
        cursor?.apply {
            return getString(getColumnIndex(COLUMN_VIEW_TYPE)).toInt()
        }
        return IGNORE_ITEM_VIEW_TYPE
    }

    override fun getItemViewType(position: Int): Int {
        return getItemViewType(getItem(position))
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val cursor: Cursor = getItem(position)
        val view: View = newView(context, cursor, parent)

        bindView(view, context, cursor)

        return view
    }

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return newView(cursor)
    }

    private fun newView(cursor: Cursor?): View {
        return when (val itemViewType: Int = getItemViewType(cursor)) {
            VIEW_TYPE_LOCATION_SUGGESTION, VIEW_TYPE_EMPTY_QUERY, VIEW_TYPE_CONNECTIVITY_ERROR -> {
                LocationSuggestionItemViewBinding.inflate(inflater).apply {
                    createViewHolder(root, locationName, locationIcon, itemViewType)
                }.root
            }
            VIEW_TYPE_LOCATION_RECENT_QUERY -> {
                LocationRecentQueryItemViewBinding.inflate(inflater).apply {
                    createViewHolder(root, locationName, locationIcon, itemViewType, removeRecentQueryButton)
                }.root
            }
            else -> throw IllegalArgumentException("Unknown view type: ${getItemViewType(cursor)}")
        }
    }

    private fun createViewHolder(
        root: View,
        suggestionText: TextView,
        suggestionIcon: ImageView,
        itemViewType: Int,
        removeRecentQueryButton: ImageButton? = null): ViewHolder {

        return ViewHolder().apply {

            this.suggestionText = suggestionText
            this.suggestionIcon = suggestionIcon
            this.removeRecentQueryButton = removeRecentQueryButton
            this.itemViewType = itemViewType

            root.tag = this
        }
    }

    @Suppress("Range")
    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        cursor?.apply {

            val viewHolder = view?.tag as ViewHolder

            val itemViewType: Int = viewHolder.itemViewType
            val iconRes: Int = getString(getColumnIndex(COLUMN_ICON)).toInt()
            val suggestionText: String = getString(getColumnIndex(SUGGEST_COLUMN_TEXT_1))

            viewHolder.suggestionIcon.setImageDrawable(AppCompatResources.getDrawable(context!!, iconRes))
            viewHolder.suggestionText.text = suggestionText

            if (viewHolder.itemViewType == VIEW_TYPE_LOCATION_SUGGESTION
                || viewHolder.itemViewType == VIEW_TYPE_LOCATION_RECENT_QUERY) {

                val latitude: Double = getString(getColumnIndex(COLUMN_LATITUDE)).toDouble()
                val longitude: Double = getString(getColumnIndex(COLUMN_LONGITUDE)).toDouble()
                val suggestion = LocationSuggestion(suggestionText, latitude, longitude)

                view.setOnClickListener {
                    listener.onSuggestionClick(suggestion, itemViewType)
                }
                viewHolder.removeRecentQueryButton?.setOnClickListener {
                    listener.onRemoveRecentQuery(suggestion)
                }
            }
        }
    }
}