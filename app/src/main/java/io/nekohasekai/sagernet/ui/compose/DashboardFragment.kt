package io.nekohasekai.sagernet.ui.compose

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.preference.PreferenceDataStore
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ToolbarFragment
import io.nekohasekai.sagernet.ui.compose.theme.MirageTheme

/**
 * Home / Dashboard — the first Compose vertical slice.
 *
 * Reads live state from [DataStore] / [ProfileManager] and drives connect/disconnect through
 * [SagerNet]. Kept intentionally small; the ViewModel layer is introduced in a later step.
 */
class DashboardFragment : ToolbarFragment(R.layout.layout_compose),
    OnPreferenceDataStoreChangeListener {

    private var state by mutableStateOf(DashboardState.empty())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_home)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MirageTheme {
                DashboardScreen(
                    state = state,
                    onToggleConnection = ::toggleConnection,
                    onOpenProfiles = {
                        (activity as? MainActivity)?.displayFragmentWithId(R.id.nav_configuration)
                    },
                )
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        DataStore.configurationStore.registerChangeListener(this)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        DataStore.configurationStore.unregisterChangeListener(this)
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        refresh()
    }

    private fun toggleConnection() {
        if (DataStore.serviceState.canStop) {
            SagerNet.stopService()
        } else {
            SagerNet.startService()
        }
        refresh()
    }

    private fun refresh() {
        val serviceState = DataStore.serviceState
        val profileId = DataStore.currentProfile
        val profile = if (profileId > 0) ProfileManager.getProfile(profileId) else null
        state = DashboardState(
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
}
