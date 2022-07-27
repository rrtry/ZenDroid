package ru.rrtry.silentdroid.interfaces

import ru.rrtry.silentdroid.entities.Profile
import java.util.UUID

interface ProfileActionListener: ListViewContract<Profile> {

    fun setSelection(id: UUID?)

    fun isEnabled(profile: Profile): Boolean
}