package com.omerkurdi.stednetlock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.omerkurdi.stednetlock.data.NetworkMasks
import com.omerkurdi.stednetlock.data.NetworkProfile
import com.omerkurdi.stednetlock.data.NetworkProfileDatabase
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

class NetworkSwitcherWidgetProvider : AppWidgetProvider() {

    companion object {
        const val TAG = "WidgetProvider"
        const val ACTION_CYCLE_PROFILE = "com.omerkurdi.stednetlock.ACTION_CYCLE_PROFILE"
        const val PREFS_NAME = "widget_prefs"
        const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_switcher)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = NetworkProfileDatabase.getDatabase(context, this)
                val dao = db.networkProfileDao()

                var widgetProfiles = dao.getWidgetProfiles()
                if (widgetProfiles.isEmpty()) {
                    widgetProfiles = dao.getDefaultWidgetProfiles()
                }

                if (widgetProfiles.isEmpty()) {
                    return@launch
                }

                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var activeId = sharedPrefs.getInt(KEY_ACTIVE_PROFILE_ID, -1)

                var activeProfile = widgetProfiles.find { it.id == activeId }
                if (activeProfile == null) {
                    activeProfile = widgetProfiles.first()
                    sharedPrefs.edit().putInt(KEY_ACTIVE_PROFILE_ID, activeProfile.id).apply()
                }

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_profile_name, activeProfile.name)

                    val emoji = when {
                        activeProfile.isDefaultAuto -> "📡"
                        activeProfile.is5gEnabled && !activeProfile.is4gEnabled -> "⚡"
                        activeProfile.is4gEnabled && !activeProfile.is5gEnabled -> "💨"
                        else -> "⭐"
                    }
                    views.setTextViewText(R.id.widget_icon, emoji)

                    val bgRes = when {
                        activeProfile.isDefaultAuto -> R.drawable.widget_button_bg_auto
                        activeProfile.is5gEnabled && !activeProfile.is4gEnabled -> R.drawable.widget_button_bg_5g
                        activeProfile.is4gEnabled && !activeProfile.is5gEnabled -> R.drawable.widget_button_bg_4g
                        else -> R.drawable.widget_button_bg_custom
                    }
                    views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

                    val intent = Intent(context, NetworkSwitcherWidgetProvider::class.java).apply {
                        action = ACTION_CYCLE_PROFILE
                    }
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        flags
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed updateWidget: ${e.message}", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        if (action == ACTION_CYCLE_PROFILE) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            scope.launch {
                try {
                    val db = NetworkProfileDatabase.getDatabase(context, this)
                    val dao = db.networkProfileDao()

                    var widgetProfiles = dao.getWidgetProfiles()
                    if (widgetProfiles.isEmpty()) {
                        widgetProfiles = dao.getDefaultWidgetProfiles()
                    }

                    if (widgetProfiles.isEmpty()) {
                        showToast(context, "No network profiles found!")
                        return@launch
                    }

                    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val activeId = sharedPrefs.getInt(KEY_ACTIVE_PROFILE_ID, -1)

                    var currentIndex = widgetProfiles.indexOfFirst { it.id == activeId }
                    if (currentIndex == -1) {
                        currentIndex = 0
                    }

                    val nextIndex = (currentIndex + 1) % widgetProfiles.size
                    val nextProfile = widgetProfiles[nextIndex]

                    val running = try { Shizuku.pingBinder() } catch (e: Exception) { false }
                    if (!running) {
                        showToast(context, "Shizuku is not running!")
                        return@launch
                    }

                    val granted = try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
                    if (!granted) {
                        showToast(context, "Shizuku permission not granted!")
                        return@launch
                    }

                    val success = performNetworkSwitch(context, nextProfile.getBitmask())
                    if (success) {
                        sharedPrefs.edit().putInt(KEY_ACTIVE_PROFILE_ID, nextProfile.id).apply()
                        showToast(context, "Switched to ${nextProfile.name}")

                        val widgetManager = AppWidgetManager.getInstance(context)
                        val component = ComponentName(context, NetworkSwitcherWidgetProvider::class.java)
                        val ids = widgetManager.getAppWidgetIds(component)
                        for (id in ids) {
                            updateWidget(context, widgetManager, id)
                        }
                    } else {
                        showToast(context, "Failed to apply network mode for ${nextProfile.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Cycle switch failure: ${e.message}", e)
                    showToast(context, "Error: ${e.localizedMessage}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun performNetworkSwitch(context: Context, bitmask: Long): Boolean {
        val deferredController = CompletableDeferred<IShizukuController?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                deferredController.complete(IShizukuController.Stub.asInterface(service))
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                if (!deferredController.isCompleted) {
                    deferredController.complete(null)
                }
            }
        }

        return try {
            val component = ComponentName(context.packageName, ShizukuControllerService::class.java.name)
            val args = Shizuku.UserServiceArgs(component)
                .daemon(false)
                .processNameSuffix("widget_service")

            if (!Shizuku.pingBinder()) {
                return false
            }

            Shizuku.bindUserService(args, connection)

            val controller = withTimeoutOrNull(3000) {
                deferredController.await()
            }

            if (controller != null) {
                val subId = try {
                    SubscriptionManager.getDefaultDataSubscriptionId()
                } catch (e: Exception) {
                    -1
                }
                controller.setNetworkMode(subId, bitmask)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing background switch: ${e.message}", e)
            false
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
