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
            this.putExtra(EXTRA_RINGTONE_TYPE, input)
            this.putExtra(EXTRA_RINGTONE_SHOW_DEFAULT, true)
            this.putExtra(EXTRA_RINGTONE_EXISTING_URI, if (existingUri == Uri.EMPTY)
                getActualDefaultRingtoneUri(context, input) else existingUri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI)
        } else null
    }
}