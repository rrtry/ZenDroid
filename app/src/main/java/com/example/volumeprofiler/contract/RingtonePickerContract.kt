package com.example.volumeprofiler.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import kotlin.properties.Delegates
import android.media.RingtoneManager.*

class RingtonePickerContract: ActivityResultContract<Int, Uri?>() {

    var ringtoneType by Delegates.notNull<Int>()
    var existingUri: Uri = Uri.EMPTY

    override fun createIntent(context: Context, input: Int?): Intent {
        ringtoneType = input!!
        return Intent(ACTION_RINGTONE_PICKER).apply {
            putExtra(EXTRA_RINGTONE_TYPE, input)
            putExtra(EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(EXTRA_RINGTONE_EXISTING_URI, if (existingUri == Uri.EMPTY)
                getActualDefaultRingtoneUri(context, input) else existingUri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI)
    }
}