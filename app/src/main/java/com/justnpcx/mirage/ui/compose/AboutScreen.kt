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
import androidx.compose.ui.res.stringResource
import com.justnpcx.mirage.R
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
    /** preview builds track a separate "preview" release */
    val showPreviewUpdate: Boolean = false,
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
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                    InfoRow(title = stringResource(R.string.about_check_update), onClick = { onCheckUpdate(false) })
                    if (state.showPreviewUpdate) {
                        Divider()
                        InfoRow(
                            title = stringResource(R.string.about_check_update_preview),
                            onClick = { onCheckUpdate(true) },
                        )
                    }
                    Divider()
                    InfoRow(title = stringResource(R.string.about_singbox), subtitle = state.singBoxVersion, onClick = null)
                    Divider()
                    InfoRow(
                        title = "GitHub",
                        subtitle = "justnpcx/Mirage",
                        onClick = { onOpenUrl("https://github.com/justnpcx/Mirage") },
                    )
                    Divider()
                    InfoRow(
                        title = stringResource(R.string.about_license),
                        subtitle = "GPL-3.0",
                        onClick = { onOpenUrl("https://www.gnu.org/licenses/gpl-3.0.html") },
                    )
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
                            title = stringResource(R.string.about_ignore_battery),
                            subtitle = stringResource(R.string.about_ignore_battery_sum),
                            onClick = onRequestIgnoreBatteryOptimizations,
                        )
                    }
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
