package ru.rrtry.silentdroid.entities

import android.os.Parcelable
import ru.rrtry.silentdroid.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hint(
    val text: String,
    override var id: Int = Int.MAX_VALUE,
    override val viewType: Int = R.layout.power_save_mode_hint
): ListItem<Int>, Parcelable