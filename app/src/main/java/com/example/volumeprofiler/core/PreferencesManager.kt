package com.example.volumeprofiler.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.Profile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor (@ApplicationContext private val context: Context) {

    private val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
    } else {
        context
    }
    private val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(
        SHARED_PREFS, Context.MODE_PRIVATE
    )
    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    fun isProfileEnabled(profile: Profile): Boolean {
        return profile.id == getProfile()?.id
    }

    fun getProfile(): Profile? {
        sharedPreferences.getString(PREFS_PROFILE, null)?.also {
            return gson.fromJson(it, object: TypeToken<Profile>() {}.type)
        }
        return null
    }

    fun getTriggerType(): Int {
        return sharedPreferences.getInt(PREFS_TRIGGER_TYPE, -1)
    }

    fun setManualTriggerType() {
        sharedPreferences
            .edit()
            .putInt(PREFS_TRIGGER_TYPE, TRIGGER_TYPE_MANUAL)
            .remove(PREFS_TRIGGER)
            .apply()
    }

    fun <T> getTrigger(): T {
        sharedPreferences.getString(PREFS_TRIGGER, null).also {
            return gson.fromJson(it, getType(getTriggerType()))
        }
    }

    private fun getType(triggeredBy: Int): Type {
        return when (triggeredBy) {
            TRIGGER_TYPE_ALARM -> object : TypeToken<Alarm>() {}.rawType
            TRIGGER_TYPE_GEOFENCE_ENTER, TRIGGER_TYPE_GEOFENCE_EXIT -> object : TypeToken<Location>() {}.rawType
            else -> object : TypeToken<Profile>() {}.rawType
        }
    }

    fun setProfile(profile: Profile) {
        sharedPreferences.edit()
            .putString(PREFS_PROFILE, gson.toJson(profile))
            .apply()
    }

    fun <T> setProfile(profile: Profile, triggeredBy: Int = TRIGGER_TYPE_MANUAL, trigger: T?) {
        sharedPreferences.edit()
            .putString(PREFS_PROFILE, gson.toJson(profile))
            .putInt(PREFS_TRIGGER_TYPE, triggeredBy)
            .putString(PREFS_TRIGGER, gson.toJson(trigger, getType(triggeredBy)))
            .apply()
    }

    companion object {

        internal const val TRIGGER_TYPE_MANUAL: Int = 0
        internal const val TRIGGER_TYPE_ALARM: Int = 1
        internal const val TRIGGER_TYPE_GEOFENCE_ENTER: Int = 2
        internal const val TRIGGER_TYPE_GEOFENCE_EXIT: Int = 3

        private const val SHARED_PREFS: String = "volumeprofiler_shared_prefs"
        private const val PREFS_PROFILE: String = "profile"
        private const val PREFS_TRIGGER_TYPE: String = "trigger_type"
        private const val PREFS_TRIGGER: String = "trigger"
    }
}