package com.justnpcx.mirage.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.justnpcx.mirage.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Immutable UI state for the Home / Dashboard screen.
 */
enum class ConnectionState { CONNECTED, CONNECTING, STOPPING, DISCONNECTED }

data class DashboardState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val started: Boolean = false,
    val connected: Boolean,
    val canStop: Boolean,
    val profileName: String,
    val serverAddress: String,
    val upload: String = "—",
    val download: String = "—",
) {
    companion object {
        fun empty() = DashboardState(
            connectionState = ConnectionState.DISCONNECTED,
            started = false,
            connected = false,
            canStop = false,
            profileName = "",
            serverAddress = "",
        )
    }
}

/**
 * Mirage Home screen — first Compose vertical slice (Material 3).
 */
@Composable
fun DashboardScreen(
    state: DashboardState,
    onToggleConnection: () -> Unit,
    onOpenProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(state)
            TrafficCard(state)
            ProfileCard(state, onOpenProfiles)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onToggleConnection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = if (state.canStop) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(if (state.canStop) R.string.dashboard_disconnect else R.string.dashboard_connect),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: DashboardState) {
    val stateLabel = stringResource(
        when (state.connectionState) {
            ConnectionState.CONNECTED -> R.string.dashboard_state_connected
            ConnectionState.CONNECTING -> R.string.dashboard_state_connecting
            ConnectionState.STOPPING -> R.string.dashboard_state_stopping
            ConnectionState.DISCONNECTED -> R.string.dashboard_state_disconnected
        }
    )
    val stateDetail = stringResource(
        when {
            state.connected -> R.string.dashboard_detail_routed
            state.started -> R.string.dashboard_detail_running
            else -> R.string.dashboard_detail_idle
        }
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stateDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrafficCard(state: DashboardState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TrafficMetric(stringResource(R.string.dashboard_upload), state.upload)
            TrafficMetric(stringResource(R.string.dashboard_download), state.download)
        }
    }
}

@Composable
private fun TrafficMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileCard(state: DashboardState, onOpenProfiles: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_current_node),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.profileName.ifBlank { stringResource(R.string.dashboard_no_node) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.serverAddress.isNotBlank()) {
                    Text(
                        text = state.serverAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.size(8.dp))
            OutlinedButton(onClick = onOpenProfiles) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.dashboard_switch))
            }
        }
    }
}
