package com.example.volumeprofiler.util

import com.example.volumeprofiler.R

interface SuggestionQueryColumns {

    companion object {

        const val COLUMN_LATITUDE: String = "latitude"
        const val COLUMN_LONGITUDE: String = "longitude"
        const val COLUMN_VIEW_TYPE: String = "view_type"
        const val COLUMN_ICON: String = "icon"

        const val VIEW_TYPE_LOCATION_SUGGESTION: Int = 0
        const val VIEW_TYPE_EMPTY_QUERY: Int = 1
        const val VIEW_TYPE_CONNECTIVITY_ERROR: Int = 2

        const val ICON_LOCATION_SUGGESTION: Int = R.drawable.baseline_location_on_black_24dp
        const val ICON_EMPTY_QUERY: Int = R.drawable.ic_baseline_not_listed_location_24
        const val ICON_CONNECTIVITY_ERROR: Int = R.drawable.ic_baseline_signal_wifi_off_24

        const val SUGGESTION_TEXT_EMPTY_QUERY: String = "No results"
        const val SUGGESTION_TEXT_CONNECTIVITY_ERROR: String = "Looks like you're offline"

        const val ID_EMPTY_QUERY: Int = 162
        const val ID_CONNECTIVITY_ERROR: Int = 163
    }
}