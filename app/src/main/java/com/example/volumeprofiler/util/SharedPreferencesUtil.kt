package com.example.volumeprofiler.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.collection.ArrayMap
import com.example.volumeprofiler.models.Profile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesUtil @Inject constructor (
    @ApplicationContext private val context: Context
) {

    private val sharedPreferences: SharedPreferences = (getStorageContext()).getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    private fun getStorageContext(): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }

    fun getRecyclerViewPositionsMap(): ArrayMap<UUID, Int>? {
        return Gson().fromJson(sharedPreferences.getString(
                PREFS_POSITIONS_MAP, null), object : TypeToken<ArrayMap<UUID, Int>>() {}.type
        )
    }

    fun writeProfilePositions(positionMap: ArrayMap<UUID, Int>): Unit {
        val gson: Gson = Gson()
        val str: String = gson.toJson(positionMap)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(PREFS_POSITIONS_MAP, str)
        editor.apply()
    }

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
        editor.apply()
    }

    fun getEnabledProfileTitle(): String? {
        return sharedPreferences.getString(PREFS_PROFILE_TITLE, "Select profile")
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

    fun getRingStreamVolume(): Int {
        return sharedPreferences.getInt(PREFS_PROFILE_STREAM_RING, -1)
    }

    fun isProfileEnabled(profile: Profile): Boolean {
        val id: String? = sharedPreferences.getString(PREFS_PROFILE_ID, "")
        if (id != null && profile.id.toString() == id) {
            return true
        }
        return false
    }

    fun clearActiveProfileRecord(): Unit {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear().apply()
    }

    fun getEnabledProfileId(): String? {
        return sharedPreferences.getString(PREFS_PROFILE_ID, null)
    }

    companion object {

        private const val PREFS_POSITIONS_MAP: String = "prefs_positions_map"
        const val SHARED_PREFS: String = "volumeprofiler_shared_prefs"
        const val PREFS_PROFILE_ID = "prefs_profile_id"
        const val PREFS_PROFILE_STREAM_NOTIFICATION = "prefs_profile_stream_notification"
        const val PREFS_PROFILE_STREAM_RING = "prefs_profile_streams_ring"
        const val PREFS_PROFILE_TITLE = "prefs_profile_title"
        const val PREFS_RINGER_MODE: String = "prefs_ringer_mode"
        const val PREFS_NOTIFICATION_MODE: String = "prefs_notification_mode"
    }
}