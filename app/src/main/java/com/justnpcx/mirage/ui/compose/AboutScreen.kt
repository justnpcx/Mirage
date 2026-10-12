package com.justnpcx.mirage.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.justnpcx.mirage.R

private const val RELEASES_URL = "https://github.com/justnpcx/Mirage/releases"
private const val REPO_URL = "https://github.com/justnpcx/Mirage"

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
 *
 * Reads top to bottom as: what this is (hero) -> what makes it different (highlights) -> the
 * mechanical bits (update, versions, licence) -> what it speaks (protocols). The conditional
 * cards (plugins, battery) come last so the page has a stable top on every install.
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Hero(state.appVersion, onClick = { onOpenUrl(RELEASES_URL) }) }

            item {
                SectionCard(stringResource(R.string.about_highlights)) {
                    Highlight(stringResource(R.string.about_hl_kernel))
                    Highlight(stringResource(R.string.about_hl_xhttp))
                    Highlight(stringResource(R.string.about_hl_reality))
                    Highlight(stringResource(R.string.about_hl_dns))
                    Highlight(stringResource(R.string.about_hl_core))
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                SectionCard(stringResource(R.string.about_info)) {
                    InfoRow(
                        title = stringResource(R.string.about_check_update),
                        onClick = { onCheckUpdate(false) },
                    )
                    if (state.showPreviewUpdate) {
                        Divider()
                        InfoRow(
                            title = stringResource(R.string.about_check_update_preview),
                            onClick = { onCheckUpdate(true) },
                        )
                    }
                    Divider()
                    InfoRow(
                        title = stringResource(R.string.about_singbox),
                        subtitle = state.singBoxVersion,
                        onClick = null,
                    )
                    Divider()
                    InfoRow(
                        title = "GitHub",
                        subtitle = "justnpcx/Mirage",
                        onClick = { onOpenUrl(REPO_URL) },
                    )
                    Divider()
                    InfoRow(
                        title = stringResource(R.string.about_license),
                        subtitle = "GPL-3.0",
                        onClick = { onOpenUrl("https://www.gnu.org/licenses/gpl-3.0.html") },
                    )
                }
            }

            item {
                SectionCard(stringResource(R.string.about_protocols)) {
                    Text(
                        text = stringResource(R.string.about_protocols_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    )
                }
            }

            if (state.plugins.isNotEmpty()) {
                item {
                    SectionCard("Plugins") {
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
                    PlainCard {
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

/** Name, version and one-line pitch. Tapping it opens the releases page. */
@Composable
private fun Hero(version: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Mirage",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = version,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            )
            content()
        }
    }
}

/** Card with no section header — used when the single row already reads as its own title. */
@Composable
private fun PlainCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column { content() }
    }
}

/** One bullet of the highlights card. */
@Composable
private fun Highlight(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
