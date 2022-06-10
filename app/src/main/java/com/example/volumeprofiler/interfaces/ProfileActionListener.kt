package com.example.volumeprofiler.interfaces

import com.example.volumeprofiler.entities.Profile
import java.util.UUID

interface ProfileActionListener: ListItemActionListener<Profile> {

    fun setSelection(id: UUID?)

    fun isEnabled(profile: Profile): Boolean
}