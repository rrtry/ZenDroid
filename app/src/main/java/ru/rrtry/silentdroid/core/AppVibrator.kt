package ru.rrtry.silentdroid.core

import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.*
import android.os.VibrationEffect.createOneShot
import android.os.VibrationEffect.createPredefined
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class AppVibrator @Inject constructor(@ActivityContext private val context: Context) {

    private val appVibrator: Vibrator get() = getDefaultVibrator()
    val hasVibrator: Boolean get() = appVibrator.hasVibrator()

    private fun getDefaultVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val vibratorManager: VibratorManager = context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
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
