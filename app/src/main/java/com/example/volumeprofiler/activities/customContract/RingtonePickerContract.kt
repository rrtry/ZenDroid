package com.example.volumeprofiler.activities.customContract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import kotlin.properties.Delegates

class RingtonePickerContract: ActivityResultContract<Int, Uri?>() {

    var ringtoneType by Delegates.notNull<Int>()

    override fun createIntent(context: Context, input: Int?): Intent {
        ringtoneType = input!!
        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        } else {
            null
        }
    }
}