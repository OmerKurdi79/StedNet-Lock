package com.omerkurdi.stednetlock

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omerkurdi.stednetlock.data.NetworkProfile
import com.omerkurdi.stednetlock.data.NetworkProfileDatabase
import com.omerkurdi.stednetlock.data.NetworkProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NetworkSwitcherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NetworkProfileDatabase.getDatabase(application, viewModelScope)
    private val repository = NetworkProfileRepository(db.networkProfileDao())
    val shizukuManager = ShizukuManager(application)
    
    private val sharedPrefs = application.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    // Shizuku connection state
    val isShizukuRunning = shizukuManager.isShizukuRunning
    val isPermissionGranted = shizukuManager.isPermissionGranted
    val isServiceBound = shizukuManager.isServiceBound
    val activeSims = shizukuManager.activeSubscriptions

    // Selected SIM / Subscription
    private val _selectedSimSubId = MutableStateFlow<Int>(-1)
    val selectedSimSubId: StateFlow<Int> = _selectedSimSubId

    // Local DB profiles
    val profiles: StateFlow<List<NetworkProfile>> = repository.allProfilesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dark Mode Setting (Default to dark!)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // Active Profile Synchronization
    private val _activeProfileId = MutableStateFlow(-1)
    val activeProfileId: StateFlow<Int> = _activeProfileId

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "active_profile_id") {
            _activeProfileId.value = sharedPrefs.getInt("active_profile_id", -1)
        }
    }

    init {
        _activeProfileId.value = sharedPrefs.getInt("active_profile_id", -1)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)

        // Find default active sub ID if available
        viewModelScope.launch {
            activeSims.collect { sims ->
                if (sims.isNotEmpty() && _selectedSimSubId.value == -1) {
                    _selectedSimSubId.value = sims.first().subId
                }
            }
        }
    }

    fun selectSim(subId: Int) {
        _selectedSimSubId.value = subId
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun requestPermission() {
        shizukuManager.requestPermission()
    }

    fun refreshStatus() {
        shizukuManager.checkStatus()
    }

    fun applyNetworkProfile(profile: NetworkProfile, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = shizukuManager.applyProfile(profile, _selectedSimSubId.value)
            if (success) {
                // Persist current applied ID
                sharedPrefs.edit().putInt("active_profile_id", profile.id).apply()
                _activeProfileId.value = profile.id
                updateWidgetNow()
            }
            onCompleted(success)
        }
    }

    fun addNewProfile(name: String, is2g: Boolean, is3g: Boolean, is4g: Boolean, is5g: Boolean) {
        viewModelScope.launch {
            val profile = NetworkProfile(
                name = name,
                is2gEnabled = is2g,
                is3gEnabled = is3g,
                is4gEnabled = is4g,
                is5gEnabled = is5g,
                isDefaultAuto = false,
                isSystemDefault = false
            )
            repository.insert(profile)
        }
    }

    fun deleteProfile(profile: NetworkProfile) {
        viewModelScope.launch {
            if (!profile.isSystemDefault) {
                repository.delete(profile)
                // Trigger home screen widget update
                updateWidgetNow()
            }
        }
    }

    fun toggleShowOnWidget(profileId: Int, show: Boolean) {
        viewModelScope.launch {
            repository.updateShowOnWidget(profileId, show)
            updateWidgetNow()
        }
    }

    private fun updateWidgetNow() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, NetworkSwitcherWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
            android.content.ComponentName(context, NetworkSwitcherWidgetProvider::class.java)
        )
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        shizukuManager.cleanup()
    }
}
