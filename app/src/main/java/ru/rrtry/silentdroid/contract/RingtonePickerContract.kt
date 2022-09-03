package ru.rrtry.silentdroid.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import kotlin.properties.Delegates
import android.media.RingtoneManager.*

class RingtonePickerContract: ActivityResultContract<Int, Uri?>() {
    
    var ringtoneType by Delegates.notNull<Int>()
    var uri: Uri = Uri.EMPTY

    private fun getExistingUri(context: Context, type: Int): Uri {
        return if (uri == Uri.EMPTY) {
            try {
                getActualDefaultRingtoneUri(context, type)
            } catch (e: RuntimeException) {
                Uri.EMPTY
            }
        } else {
            uri
        }
    }

    override fun createIntent(context: Context, type: Int): Intent {
        ringtoneType = type
        return Intent(ACTION_RINGTONE_PICKER).apply {
            putExtra(EXTRA_RINGTONE_TYPE, type)
            putExtra(EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(EXTRA_RINGTONE_EXISTING_URI, getExistingUri(context, type))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI)
    }
}