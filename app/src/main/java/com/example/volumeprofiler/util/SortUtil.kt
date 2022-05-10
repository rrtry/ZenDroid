package com.example.volumeprofiler.util

import androidx.collection.ArrayMap
import com.example.volumeprofiler.entities.Profile
import java.util.*

fun restoreChangedPositions(list: List<Profile>, positionMap: ArrayMap<UUID, Int>): List<Profile> {
    if (positionMap.isNotEmpty()) {
        val arrayList: ArrayList<Profile> = list as ArrayList<Profile>
        arrayList.sortWith(object : Comparator<Profile> {

            override fun compare(o1: Profile?, o2: Profile?): Int {
                if (o1 != null && o2 != null) {
                    if (positionMap.containsKey(o1.id) && positionMap.containsKey(o2.id)) {
                        return positionMap[o1.id]!! - positionMap[o2.id]!!
                    }
                    return 0
                }
                return 0
            }
        })
        return arrayList
    }
    return list
}