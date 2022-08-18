package ru.rrtry.silentdroid.core

import android.content.Context
import android.os.*
import android.os.VibrationEffect.createOneShot
import android.os.VibrationEffect.createPredefined
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class AppVibrator @Inject constructor(@ActivityContext private val context: Context) {

    private val appVibrator: Vibrator
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            return vibratorManager.defaultVibrator
        }
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun hasVibrator(): Boolean {
        return appVibrator.hasVibrator()
    }

    fun createVibrationEffect() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                appVibrator.vibrate(createPredefined(VibrationEffect.EFFECT_CLICK))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                appVibrator.vibrate(createOneShot(VIBRATION_DEFAULT_LENGTH, VIBRATION_DEFAULT_AMPLITUDE))
            }
            else -> {
                appVibrator.vibrate(VIBRATION_DEFAULT_LENGTH)
            }
        }
    }

    companion object {

        private const val VIBRATION_DEFAULT_LENGTH: Long = 100
        private const val VIBRATION_DEFAULT_AMPLITUDE: Int = 100
    }
}
