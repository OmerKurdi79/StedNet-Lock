package com.omerkurdi.stednetlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_profiles")
data class NetworkProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val is2gEnabled: Boolean,
    val is3gEnabled: Boolean,
    val is4gEnabled: Boolean,
    val is5gEnabled: Boolean,
    val isDefaultAuto: Boolean = false,
    val isSystemDefault: Boolean = false,
    val showOnWidget: Boolean = false
) {
    fun getBitmask(): Long {
        if (isDefaultAuto) {
            return NetworkMasks.MASK_ALL
        }
        var mask = 0L
        if (is2gEnabled) mask = mask or NetworkMasks.MASK_2G
        if (is3gEnabled) mask = mask or NetworkMasks.MASK_3G
        if (is4gEnabled) mask = mask or NetworkMasks.MASK_4G
        if (is5gEnabled) mask = mask or NetworkMasks.MASK_5G
        return mask
    }

    fun getTechnologiesString(): String {
        if (isDefaultAuto) return "2G, 3G, 4G, 5G (Auto Reset)"
        val list = mutableListOf<String>()
        if (is5gEnabled) list.add("5G")
        if (is4gEnabled) list.add("4G")
        if (is3gEnabled) list.add("3G")
        if (is2gEnabled) list.add("2G")
        return if (list.isEmpty()) "None Selected" else list.joinToString(" + ")
    }
}
