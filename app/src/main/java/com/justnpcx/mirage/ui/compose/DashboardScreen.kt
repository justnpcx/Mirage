package com.justnpcx.mirage.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.justnpcx.mirage.R

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
    /** recent rates (bytes/s), oldest first, for the sparklines */
    val txHistory: List<Long> = emptyList(),
    val rxHistory: List<Long> = emptyList(),
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
 * Mirage Home — Material 3, with an animated connection hero and live traffic sparklines.
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ConnectionHero(state, onToggleConnection)
            TrafficCard(state)
            ProfileCard(state, onOpenProfiles)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ---------------------------------------------------------------- hero */

@Composable
private fun ConnectionHero(state: DashboardState, onToggleConnection: () -> Unit) {
    val connected = state.connected
    val active = connected || state.connectionState == ConnectionState.CONNECTING

    val stateLabel = stringResource(stateLabelRes(state.connectionState))
    val stateDetail = stringResource(stateDetailRes(state))

    val accent by animateColorAsState(
        targetValue = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(600),
        label = "accent",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 260f),
        label = "scale",
    )

    // a slow outward "breathing" pulse while the tunnel is up
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp),
            contentAlignment = Alignment.Center,
        ) {
            ConnectionRing(active = active, pulse = pulse, accent = accent)
            Surface(
                onClick = onToggleConnection,
                shape = CircleShape,
                color = if (connected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (connected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.size((156 * scale).dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (state.canStop) R.string.dashboard_disconnect else R.string.dashboard_connect
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Text(
            text = stateDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun stateLabelRes(connectionState: ConnectionState): Int = when (connectionState) {
    ConnectionState.CONNECTED -> R.string.dashboard_state_connected
    ConnectionState.CONNECTING -> R.string.dashboard_state_connecting
    ConnectionState.STOPPING -> R.string.dashboard_state_stopping
    ConnectionState.DISCONNECTED -> R.string.dashboard_state_disconnected
}

private fun stateDetailRes(state: DashboardState): Int = when {
    state.connected -> R.string.dashboard_detail_routed
    state.started -> R.string.dashboard_detail_running
    else -> R.string.dashboard_detail_idle
}

@Composable
private fun ConnectionRing(active: Boolean, pulse: Float, accent: Color) {
    Canvas(modifier = Modifier.size(232.dp)) {
        val radius = size.minDimension / 2f
        if (active) {
            drawCircle(
                color = accent.copy(alpha = 0.22f * (1f - pulse)),
                radius = radius * (0.62f + 0.36f * pulse),
            )
        }
        drawCircle(
            color = accent.copy(alpha = 0.30f),
            radius = radius * 0.72f,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/* ------------------------------------------------------------- traffic */

@Composable
private fun TrafficCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            TrafficRow(
                label = stringResource(R.string.dashboard_upload),
                value = state.upload,
                values = state.txHistory,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(18.dp))
            TrafficRow(
                label = stringResource(R.string.dashboard_download),
                value = state.download,
                values = state.rxHistory,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TrafficRow(label: String, value: String, values: List<Long>, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Sparkline(
            values = values,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        )
    }
}

/** A minimal line chart of recent transfer rates. */
@Composable
private fun Sparkline(values: List<Long>, color: Color, modifier: Modifier) {
    val idle = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val lineWidth = 2.dp.toPx()
        if (values.size < 2) {
            drawLine(
                color = idle,
                start = Offset(0f, size.height - lineWidth),
                end = Offset(size.width, size.height - lineWidth),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
            return@Canvas
        }
        val peak = values.max().coerceAtLeast(1L).toFloat()
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value / peak) * (size.height - lineWidth) - lineWidth
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/* ------------------------------------------------------------- profile */

@Composable
private fun ProfileCard(state: DashboardState, onOpenProfiles: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_current_node),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.profileName.ifBlank { stringResource(R.string.dashboard_no_node) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.serverAddress.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.serverAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onOpenProfiles) {
                Text(stringResource(R.string.dashboard_switch))
            }
        }
    }
}
