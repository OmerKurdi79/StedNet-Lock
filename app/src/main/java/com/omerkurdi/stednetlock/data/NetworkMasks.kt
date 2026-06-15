package com.omerkurdi.stednetlock.data

object NetworkMasks {
    private fun getMask(field: String, fallback: Long): Long {
        return try {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField(field)
                .getLong(null)
        } catch (e: Exception) {
            fallback
        }
    }

    // Individual standard network bands with reflection and correct hardcoded fallbacks
    val GSM = getMask("NETWORK_TYPE_BITMASK_GSM", 1L shl 16)
    val GPRS = getMask("NETWORK_TYPE_BITMASK_GPRS", 1L shl 1)
    val EDGE = getMask("NETWORK_TYPE_BITMASK_EDGE", 1L shl 2)
    val CDMA = getMask("NETWORK_TYPE_BITMASK_CDMA", 1L shl 4)
    val TYPE_1xRTT = getMask("NETWORK_TYPE_BITMASK_1xRTT", 1L shl 7)

    val UMTS = getMask("NETWORK_TYPE_BITMASK_UMTS", 1L shl 3)
    val HSDPA = getMask("NETWORK_TYPE_BITMASK_HSDPA", 1L shl 8)
    val HSUPA = getMask("NETWORK_TYPE_BITMASK_HSUPA", 1L shl 9)
    val HSPA = getMask("NETWORK_TYPE_BITMASK_HSPA", 1L shl 10)
    val EVDO_0 = getMask("NETWORK_TYPE_BITMASK_EVDO_0", 1L shl 5)
    val EVDO_A = getMask("NETWORK_TYPE_BITMASK_EVDO_A", 1L shl 6)
    val EVDO_B = getMask("NETWORK_TYPE_BITMASK_EVDO_B", 1L shl 12)
    val EHRPD = getMask("NETWORK_TYPE_BITMASK_EHRPD", 1L shl 14)
    val HSPAP = getMask("NETWORK_TYPE_BITMASK_HSPAP", 1L shl 15)
    val TD_SCDMA = getMask("NETWORK_TYPE_BITMASK_TD_SCDMA", 1L shl 18)
    val WCDMA = getMask("NETWORK_TYPE_BITMASK_WCDMA", 1L shl 17)

    val LTE = getMask("NETWORK_TYPE_BITMASK_LTE", 1L shl 11)
    val LTE_CA = getMask("NETWORK_TYPE_BITMASK_LTE_CA", 1L shl 19)
    val IWLAN = getMask("NETWORK_TYPE_BITMASK_IWLAN", 1L shl 13)

    val NR = getMask("NETWORK_TYPE_BITMASK_NR", 1L shl 20) // 5G

    // Combined Standard Groups
    val MASK_2G = GSM or GPRS or EDGE or CDMA or TYPE_1xRTT
    val MASK_3G = UMTS or HSDPA or HSUPA or HSPA or EVDO_0 or EVDO_A or EVDO_B or EHRPD or HSPAP or TD_SCDMA or WCDMA
    val MASK_4G = LTE or LTE_CA or IWLAN
    val MASK_5G = NR

    // All-Inclusive Auto Default
    val MASK_ALL = MASK_2G or MASK_3G or MASK_4G or MASK_5G
}
