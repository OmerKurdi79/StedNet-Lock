package com.omerkurdi.stednetlock

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import com.omerkurdi.stednetlock.data.NetworkProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) {

    private val TAG = "ShizukuManager"

    private var controller: IShizukuController? = null

    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound

    private val _activeSubscriptions = MutableStateFlow<List<SimInfo>>(emptyList())
    val activeSubscriptions: StateFlow<List<SimInfo>> = _activeSubscriptions

    data class SimInfo(
        val subId: Int,
        val displayName: String,
        val slotIndex: Int,
        val number: String = ""
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Shizuku user service connected")
            controller = IShizukuController.Stub.asInterface(service)
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Shizuku user service disconnected")
            controller = null
            _isServiceBound.value = false
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _isPermissionGranted.value = granted
        if (granted) {
            bindService()
        }
    }

    init {
        checkStatus()
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add permission listener", e)
        }
    }

    fun checkStatus() {
        val checkRunning = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        _isShizukuRunning.value = checkRunning

        if (checkRunning) {
            val checkPerm = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
            _isPermissionGranted.value = checkPerm
            if (checkPerm && !_isServiceBound.value) {
                bindService()
            }
        } else {
            _isPermissionGranted.value = false
        }

        refreshSubscriptions()
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request permission failed", e)
        }
    }

    fun bindService() {
        val checkRunning = try { Shizuku.pingBinder() } catch (e: Exception) { false }
        if (!checkRunning) return
        try {
            val component = ComponentName(context.packageName, ShizukuControllerService::class.java.name)
            val args = Shizuku.UserServiceArgs(component)
                .daemon(false)
                .processNameSuffix("network_service")
            
            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            Log.e(TAG, "Binding Shizuku service failed", e)
        }
    }

    fun unbindService() {
        if (_isServiceBound.value) {
            try {
                _isServiceBound.value = false
                controller = null
            } catch (e: Exception) {
                Log.e(TAG, "Unbind failed", e)
            }
        }
    }

    fun refreshSubscriptions() {
        val list = mutableListOf<SimInfo>()
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList: List<SubscriptionInfo>? = try {
                    subscriptionManager.activeSubscriptionInfoList
                } catch (e: SecurityException) {
                    null
                }
                if (activeList != null) {
                    for (info in activeList) {
                        list.add(
                            SimInfo(
                                subId = info.subscriptionId,
                                displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                                slotIndex = info.simSlotIndex,
                                number = info.number ?: ""
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh subscription list failed", e)
        }

        if (list.isEmpty()) {
            list.add(SimInfo(subId = -1, displayName = "Default SIM / Data SIM", slotIndex = 0))
        }
        _activeSubscriptions.value = list
    }

    fun applyProfile(profile: NetworkProfile, subId: Int): Boolean {
        val activeSubId = if (subId == -1) {
            try {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } catch (e: Exception) {
                -1
            }
        } else {
            subId
        }
        val targetController = controller
        if (targetController == null) {
            Log.e(TAG, "Controller not bound, can't apply profile")
            return false
        }
        val bitmask = profile.getBitmask()
        return try {
            targetController.setNetworkMode(activeSubId, bitmask)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception applying profile", e)
            false
        }
    }

    fun cleanup() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove permission listener", e)
        }
    }
}
