package com.example.volumeprofiler.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.collection.ArrayMap
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class SharedPreferencesUtil private constructor (context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    fun getActiveProfileId(): String? = sharedPreferences.getString(PREFS_PROFILE_ID, null)

    fun getActiveProfileTitle(): String? = sharedPreferences.getString(AlarmReceiver.PREFS_PROFILE_TITLE, "Select profile")

    fun getRecyclerViewPositionsMap(): ArrayMap<UUID, Int>? {
        return Gson().fromJson(sharedPreferences.getString(
                PREFS_POSITIONS_MAP, null), object : TypeToken<ArrayMap<UUID, Int>>() {}.type
        )
    }

    fun saveRecyclerViewPositionsMap(positionMap: ArrayMap<UUID, Int>): Unit {
        val gson: Gson = Gson()
        val str: String = gson.toJson(positionMap)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(PREFS_POSITIONS_MAP, str)
        editor.apply()
    }

    fun saveProfileToSharedPrefs(id: UUID, primarySettings: Map<Int, Int>, optionalSettings: Map<String, Int>, title: String): Unit {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        for ((key, value) in primarySettings) {
            when (key) {
                AudioManager.STREAM_MUSIC -> {
                    editor.putInt(PREFS_PROFILE_STREAM_MUSIC, value)
                }
                AudioManager.STREAM_VOICE_CALL -> {
                    editor.putInt(PREFS_PROFILE_STREAM_VOICE_CALL, value)
                }
                AudioManager.STREAM_RING -> {
                    editor.putInt(PREFS_PROFILE_STREAM_RING, value)
                }
                AudioManager.STREAM_NOTIFICATION -> {
                    editor.putInt(PREFS_PROFILE_STREAM_NOTIFICATION, value)
                }
                AudioManager.STREAM_ALARM -> {
                    editor.putInt(PREFS_PROFILE_STREAM_ALARM, value)
                }
            }
        }
        editor.putString(PREFS_PROFILE_TITLE, title)
        editor.putString(PREFS_PROFILE_ID, id.toString())
        editor.apply()
    }

    fun isProfileActive(profile: Profile): Boolean {
        val id: String? = sharedPreferences.getString(AlarmReceiver.PREFS_PROFILE_ID, "")
        if (id != null && profile.id.toString() == id) {
            return true
        }
        return false
    }

    fun clearActiveProfileRecord(id: UUID): Unit {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear().apply()
    }

    companion object {

        private const val PREFS_POSITIONS_MAP: String = "prefs_positions_map"
        const val SHARED_PREFS: String = "volumeprofiler_shared_prefs"
        const val PREFS_PROFILE_ID = "prefs_profile_id"
        const val PREFS_PROFILE_STREAM_ALARM = "prefs_profile_stream_alarm"
        const val PREFS_PROFILE_STREAM_VOICE_CALL = "prefs_profile_voice_call"
        const val PREFS_PROFILE_STREAM_MUSIC = "prefs_profile_stream_music"
        const val PREFS_PROFILE_STREAM_NOTIFICATION = "prefs_profile_stream_notification"
        const val PREFS_PROFILE_STREAM_RING = "prefs_profile_streams_ring"
        const val PREFS_PROFILE_TITLE = "prefs_profile_title"

        private var INSTANCE: SharedPreferencesUtil? = null

        fun getInstance(): SharedPreferencesUtil {

            if (INSTANCE != null) {
                return INSTANCE!!
            }
            else {
                throw IllegalStateException("Singleton must be initialized")
            }
        }

        fun initialize(context: Context) {

            if (INSTANCE == null) {
                INSTANCE = SharedPreferencesUtil(context)
            }
        }
    }
}