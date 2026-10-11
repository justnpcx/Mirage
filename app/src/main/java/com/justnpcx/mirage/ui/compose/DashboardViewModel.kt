package com.justnpcx.mirage.ui.compose

import android.app.Application
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.justnpcx.mirage.SagerNet
import com.justnpcx.mirage.bg.BaseService
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Compose Home screen — the first screen moved to the
 * unidirectional-data-flow (ViewModel + StateFlow) architecture.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardState.empty())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        // the service pushes transfer rates to MainActivity; surface them on the dashboard
        viewModelScope.launch {
            SpeedRegistry.speed.collect { speed ->
                _state.value = _state.value.copy(
                    upload = formatRate(speed.txRate),
                    download = formatRate(speed.rxRate),
                )
            }
        }
    }

    private fun formatRate(bytesPerSecond: Long): String =
        if (bytesPerSecond <= 0L) {
            "—"
        } else {
            Formatter.formatFileSize(getApplication(), bytesPerSecond) + "/s"
        }

    fun refresh() {
        val serviceState = DataStore.serviceState
        val profileId = DataStore.currentProfile
        val profile = if (profileId > 0) ProfileManager.getProfile(profileId) else null
        _state.value = DashboardState(
            stateLabel = when (serviceState) {
                BaseService.State.Connected -> "Connected"
                BaseService.State.Connecting -> "Connecting…"
                BaseService.State.Stopping -> "Stopping…"
                else -> "Not connected"
            },
            stateDetail = when {
                serviceState.connected -> "Traffic is routed through Mirage"
                serviceState.started -> "Service is running"
                else -> "Tap below to start"
            },
            connected = serviceState.connected,
            canStop = serviceState.canStop,
            profileName = profile?.displayName() ?: "No node selected",
            serverAddress = profile?.displayAddress() ?: "",
            // keep the last rates while connected, blank them out otherwise
            upload = if (serviceState.connected) _state.value.upload else "—",
            download = if (serviceState.connected) _state.value.download else "—",
        )
    }

    fun toggleConnection() {
        if (DataStore.serviceState.canStop) {
            SagerNet.stopService()
        } else {
            SagerNet.startService()
        }
        refresh()
    }
}
