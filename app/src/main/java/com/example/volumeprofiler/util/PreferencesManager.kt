package com.example.volumeprofiler.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.collection.ArrayMap
import com.example.volumeprofiler.entities.Profile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor (
    @ApplicationContext private val context: Context
) {

    private val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else {
        context
    }

    private val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(
        SHARED_PREFS, Context.MODE_PRIVATE
    )

    fun writeCurrentProfileProperties(profile: Profile): Unit {
        val title: String = profile.title
        val id: UUID = profile.id
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt(PREFS_PROFILE_STREAM_NOTIFICATION, profile.notificationVolume)
        editor.putInt(PREFS_PROFILE_STREAM_RING, profile.ringVolume)
        editor.putString(PREFS_PROFILE_TITLE, title)
        editor.putString(PREFS_PROFILE_ID, id.toString())
        editor.putInt(PREFS_RINGER_MODE, profile.ringerMode)
        editor.putInt(PREFS_NOTIFICATION_MODE, profile.notificationMode)
        editor.putInt(PREFS_INTERRUPTION_FILTER, profile.interruptionFilter)
        editor.putInt(PREFS_PRIORITY_CATEGORIES, profile.priorityCategories)
        editor.putBoolean(PREFS_STREAMS_UNLINKED, profile.streamsUnlinked)
        editor.apply()
    }

    fun showPermissionExplanationDialog(): Boolean {
        return sharedPreferences.getBoolean(PREFS_SHOW_EXPLANATION_DIALOG, true)
    }

    fun getPriorityCategories(): Int {
        return sharedPreferences.getInt(PREFS_PRIORITY_CATEGORIES, -1)
    }

    fun getStreamsUnlinked(): Boolean {
        return sharedPreferences.getBoolean(PREFS_STREAMS_UNLINKED, false)
    }

    fun getRingerMode(): Int {
        return sharedPreferences.getInt(PREFS_RINGER_MODE, -1)
    }

    fun getNotificationMode(): Int {
        return sharedPreferences.getInt(PREFS_NOTIFICATION_MODE, -1)
    }

    fun getNotificationStreamVolume(): Int {
        return sharedPreferences.getInt(PREFS_PROFILE_STREAM_NOTIFICATION, -1)
    }

    fun getInterruptionFilter(): Int {
        return sharedPreferences.getInt(PREFS_INTERRUPTION_FILTER, -1)
    }

    fun getRingStreamVolume(): Int {
        return sharedPreferences.getInt(PREFS_PROFILE_STREAM_RING, -1)
    }

    fun isProfileEnabled(profile: Profile): Boolean {
        sharedPreferences.getString(PREFS_PROFILE_ID, null)?.let {
            return profile.id.toString() == it
        }
        return false
    }

    fun getRecyclerViewPositionsMap(): ArrayMap<UUID, Int>? {
        return Gson().fromJson(sharedPreferences.getString(
            PREFS_POSITIONS_MAP, null), object : TypeToken<ArrayMap<UUID, Int>>() {}.type
        )
    }

    fun putProfilePositions(positionMap: ArrayMap<UUID, Int>): Unit {
        val gson: Gson = Gson()
        val str: String = gson.toJson(positionMap)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(PREFS_POSITIONS_MAP, str)
        editor.apply()
    }

    fun clearPreferences(): Unit {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear().apply()
    }

    companion object {

        private const val SHARED_PREFS: String = "volumeprofiler_shared_prefs"
        private const val PREFS_PROFILE_ID = "prefs_profile_id"
        private const val PREFS_PROFILE_STREAM_NOTIFICATION = "prefs_profile_stream_notification"
        private const val PREFS_PROFILE_STREAM_RING = "prefs_profile_streams_ring"
        private const val PREFS_PROFILE_TITLE = "prefs_profile_title"
        private const val PREFS_SHOW_EXPLANATION_DIALOG: String = "prefs_show_explanation_dialog"
        private const val PREFS_RINGER_MODE: String = "prefs_ringer_mode"
        private const val PREFS_NOTIFICATION_MODE: String = "prefs_notification_mode"
        private const val PREFS_INTERRUPTION_FILTER: String = "prefs_interruption_filter"
        private const val PREFS_PRIORITY_CATEGORIES: String = "priority_categories"
        private const val PREFS_STREAMS_UNLINKED: String = "prefs_streams_unlinked"

        private const val PREFS_POSITIONS_MAP: String = "prefs_positions_map"
    }
}