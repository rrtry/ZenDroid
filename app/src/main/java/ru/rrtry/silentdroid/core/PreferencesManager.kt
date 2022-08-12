package ru.rrtry.silentdroid.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.Profile
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
        sharedPreferences.getString(PREFS_PROFILE, null)?.let {
            return gson.fromJson(
                it,
                object: TypeToken<Profile>() {}.type)
        }
        return null
    }

    fun getTriggerType(): Int {
        return sharedPreferences.getInt(PREFS_TRIGGER_TYPE, TRIGGER_TYPE_MANUAL)
    }

    fun <T> getTrigger(): T {
        sharedPreferences.getString(PREFS_TRIGGER, null).also {
            return gson.fromJson(it, getType(getTriggerType()))
        }
    }

    private fun getType(triggerType: Int): Type {
        return when (triggerType) {
            TRIGGER_TYPE_ALARM -> object : TypeToken<Alarm>() {}.rawType
            TRIGGER_TYPE_GEOFENCE_ENTER, TRIGGER_TYPE_GEOFENCE_EXIT -> object : TypeToken<Location>() {}.rawType
            TRIGGER_TYPE_MANUAL -> object : TypeToken<Profile>() {}.rawType
            else -> throw IllegalArgumentException("Unknown trigger type: $triggerType")
        }
    }

    private fun <T> setTrigger(editor: SharedPreferences.Editor, triggerType: Int, trigger: T?) {
        editor.apply {
            if (triggerType == TRIGGER_TYPE_MANUAL) {
                remove(PREFS_TRIGGER)
                putInt(PREFS_TRIGGER_TYPE, triggerType)
            } else {
                putInt(PREFS_TRIGGER_TYPE, triggerType)
                putString(PREFS_TRIGGER, gson.toJson(trigger, getType(triggerType)))
            }
        }
    }

    private fun <T> setTrigger(triggerType: Int, trigger: T?) {
        sharedPreferences.edit().apply {
            setTrigger(triggerType, trigger)
            apply()
        }
    }

    fun isFirstSetup(): Boolean {
        return sharedPreferences.getBoolean(PREFS_FIRST_SETUP, true)
    }

    fun getNotificationStreamType(): Int {
        return sharedPreferences.getInt(
            PREFS_NOTIFICATION_STREAM_TYPE,
            PREFS_STREAM_TYPE_NOT_SET
        )
    }

    fun setNotificationStreamType(type: Int) {
        sharedPreferences
            .edit()
            .putInt(PREFS_NOTIFICATION_STREAM_TYPE, type)
            .apply()
    }

    fun setFirstSetup() {
        sharedPreferences
            .edit()
            .putBoolean(PREFS_FIRST_SETUP, false)
            .apply()
    }

    fun setProfile(profile: Profile) {
        sharedPreferences.edit()
            .putString(PREFS_PROFILE, gson.toJson(profile))
            .apply()
    }

    fun <T> setProfile(profile: Profile, triggerType: Int = TRIGGER_TYPE_MANUAL, trigger: T?) {
        sharedPreferences.edit().apply {
            putString(PREFS_PROFILE, gson.toJson(profile))
            setTrigger(this, triggerType, trigger)
            if (triggerType == TRIGGER_TYPE_MANUAL) {
                putLong(PREFS_PROFILE_TIME, System.currentTimeMillis())
            }
            apply()
        }
    }

    fun clearPreferences() {
        sharedPreferences
            .edit()
            .clear()
            .apply()
    }

    companion object {

        internal const val PREFS_STREAM_TYPE_NOT_SET: Int = -1
        internal const val PREFS_STREAM_TYPE_INDEPENDENT: Int = 0
        internal const val PREFS_STREAM_TYPE_ALIAS: Int = 1

        internal const val TRIGGER_TYPE_MANUAL: Int = 0
        internal const val TRIGGER_TYPE_ALARM: Int = 1
        internal const val TRIGGER_TYPE_GEOFENCE_ENTER: Int = 2
        internal const val TRIGGER_TYPE_GEOFENCE_EXIT: Int = 3

        private const val SHARED_PREFS: String = "zendroid_shared_prefs"
        private const val PREFS_PROFILE: String = "profile"
        private const val PREFS_PROFILE_TIME: String = "time"
        private const val PREFS_TRIGGER_TYPE: String = "trigger_type"
        private const val PREFS_TRIGGER: String = "trigger"
        private const val PREFS_FIRST_SETUP: String = "first_setup"
        private const val PREFS_NOTIFICATION_STREAM_TYPE: String = "stream_type"
    }
}