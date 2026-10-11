package io.nekohasekai.sagernet.ui.compose

import androidx.lifecycle.ViewModel
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder for the Compose Home screen — the first screen moved to the
 * unidirectional-data-flow (ViewModel + StateFlow) architecture.
 */
class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardState.empty())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

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
