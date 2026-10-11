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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val license: String = "",
    /** BCP-47 tag, or "" for follow-system */
    val languageTag: String = "",
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
                    Divider()
                    InfoRow(title = stringResource(R.string.about_check_update_preview), onClick = { onCheckUpdate(true) })
                    Divider()
                    InfoRow(title = stringResource(R.string.about_singbox), subtitle = state.singBoxVersion, onClick = null)
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

            item {
                AboutCard {
                    InfoRow(title = "GitHub", onClick = { onOpenUrl("https://github.com/justnpcx/Mirage") })
                }
            }

            item {
                Text(
                    text = stringResource(R.string.about_license),
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
private fun LanguageDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_english),
        "zh-CN" to stringResource(R.string.language_chinese),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_language)) },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    TextButton(
                        onClick = { onPick(tag) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (tag == current) "✓  $label" else label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
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
