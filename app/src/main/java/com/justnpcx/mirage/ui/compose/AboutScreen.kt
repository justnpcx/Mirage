package com.justnpcx.mirage.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class PluginInfo(
    val pluginId: String,
    val displayName: String,
    val version: String,
    val packageName: String,
)

data class AboutState(
    val appVersion: String,
    val singBoxVersion: String,
    val plugins: List<PluginInfo> = emptyList(),
    val needBatteryOptimization: Boolean = false,
    val license: String = "",
)

/**
 * Compose replacement for the old `material-about-library` screen.
 */
@Composable
fun AboutScreen(
    state: AboutState,
    onOpenUrl: (String) -> Unit,
    onCheckUpdate: (preview: Boolean) -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    onOpenPluginSettings: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AboutCard {
                    InfoRow(
                        title = "Mirage",
                        subtitle = state.appVersion,
                        onClick = { onOpenUrl("https://github.com/justnpcx/Mirage/releases") },
                    )
                    Divider()
                    InfoRow(title = "Check for updates", onClick = { onCheckUpdate(false) })
                    Divider()
                    InfoRow(title = "Check for preview updates", onClick = { onCheckUpdate(true) })
                    Divider()
                    InfoRow(title = "sing-box", subtitle = state.singBoxVersion, onClick = null)
                }
            }

            if (state.plugins.isNotEmpty()) {
                item {
                    AboutCard {
                        state.plugins.forEachIndexed { index, plugin ->
                            if (index > 0) Divider()
                            InfoRow(
                                title = "${plugin.pluginId} (${plugin.displayName})",
                                subtitle = "v${plugin.version}",
                                onClick = { onOpenPluginSettings(plugin.packageName) },
                            )
                        }
                    }
                }
            }

            if (state.needBatteryOptimization) {
                item {
                    AboutCard {
                        InfoRow(
                            title = "Ignore battery optimizations",
                            subtitle = "Keeps the proxy service alive in the background",
                            onClick = onRequestIgnoreBatteryOptimizations,
                        )
                    }
                }
            }

            item {
                AboutCard {
                    InfoRow(title = "GitHub", onClick = { onOpenUrl("https://github.com/justnpcx/Mirage") })
                }
            }

            item {
                Text(
                    text = "License",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = state.license,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
