package com.example.volumeprofiler.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.volumeprofiler.entities.Profile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
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
        return profile.id == getEnabledProfile()?.id
    }

    fun getEnabledProfile(): Profile? {
        sharedPreferences.getString(PREFS_PROFILE, null)?.also {
            return gson.fromJson(it, object: TypeToken<Profile>() {}.type)
        }
        return null
    }

    fun setEnabledProfile(profile: Profile) {
        sharedPreferences.edit()
            .putString(PREFS_PROFILE, gson.toJson(profile))
            .apply()
    }

    fun clearPreferences() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }

    companion object {

        private const val SHARED_PREFS: String = "volumeprofiler_shared_prefs"
        private const val PREFS_PROFILE = "profile"
    }
}