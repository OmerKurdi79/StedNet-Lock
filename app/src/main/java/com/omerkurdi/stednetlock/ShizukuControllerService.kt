package com.omerkurdi.stednetlock

import android.content.Context
import android.os.IBinder
import com.omerkurdi.stednetlock.IShizukuController

class ShizukuControllerService : IShizukuController.Stub {
    constructor() : super()
    constructor(context: Context) : super()

    // Retrieve the telephone binder service via reflection
    private val telephonyService: Any? by lazy {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "phone") as IBinder?
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                asInterfaceMethod.invoke(null, binder)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun setNetworkMode(subId: Int, bitmask: Long) {
        val service = telephonyService ?: return
        try {
            val method = service.javaClass.getMethod(
                "setAllowedNetworkTypesForReason",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
            val reasonUser = 0 // TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
            method.invoke(service, subId, reasonUser, bitmask)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
