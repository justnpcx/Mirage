package com.justnpcx.mirage.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.justnpcx.mirage.BuildConfig
import com.justnpcx.mirage.R
import com.justnpcx.mirage.SagerNet
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.ktx.*
import com.justnpcx.mirage.plugin.PluginManager.loadString
import com.justnpcx.mirage.ui.compose.AboutScreen
import com.justnpcx.mirage.ui.compose.AboutState
import com.justnpcx.mirage.ui.compose.PluginInfo
import com.justnpcx.mirage.ui.compose.theme.MirageTheme
import com.justnpcx.mirage.utils.PackageCache
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins
import moe.matsuri.nb4a.utils.Util
import org.json.JSONArray

/**
 * About screen — Compose implementation (replaces the old `material-about-library` screen).
 */
class AboutFragment : ToolbarFragment(R.layout.layout_compose) {

    private companion object {
        const val RELEASES_API = "https://api.github.com/repos/justnpcx/Mirage/releases?per_page=30"

        /** tag of the optional preview channel; it has no release most of the time */
        const val PREVIEW_TAG = "preview"
    }

    private val requestIgnoreBatteryOptimizations = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    private var state by mutableStateOf(AboutState(appVersion = "", singBoxVersion = ""))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_about)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MirageTheme {
                AboutScreen(
                    state = state,
                    onOpenUrl = { url -> requireContext().launchCustomTab(url) },
                    onCheckUpdate = ::checkUpdate,
                    onRequestIgnoreBatteryOptimizations = ::requestIgnoreBattery,
                    onOpenPluginSettings = { packageName ->
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", packageName, null))
                        )
                    },
                )
            }
        }
        refresh()
    }

    private fun refresh() {
        runOnDefaultDispatcher {
            PackageCache.awaitLoadSync()
            val plugins = PackageCache.installedPluginPackages.values.mapNotNull { pkg ->
                runCatching {
                    val pluginId = pkg.providers?.get(0)?.loadString(Plugins.METADATA_KEY_ID)
                    if (pluginId.isNullOrBlank()) {
                        null
                    } else {
                        PluginInfo(
                            pluginId = pluginId,
                            displayName = Plugins.displayExeProvider(pkg.packageName),
                            version = pkg.versionName.orEmpty(),
                            packageName = pkg.packageName,
                        )
                    }
                }.getOrNull()
            }

            val needBattery = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                false
            } else {
                runCatching {
                    val pm = SagerNet.application
                        .getSystemService(Context.POWER_SERVICE) as PowerManager
                    pm.isIgnoringBatteryOptimizations(SagerNet.application.packageName).not()
                }.getOrDefault(false)
            }

            onMainDispatcher {
                state = AboutState(
                    appVersion = SagerNet.appVersionNameForDisplay,
                    singBoxVersion = Libcore.versionBox(),
                    plugins = plugins,
                    needBatteryOptimization = needBattery,
                    showPreviewUpdate = isPreview,
                )
            }
        }
    }

    private fun requestIgnoreBattery() {
        requestIgnoreBatteryOptimizations.launch(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:${SagerNet.application.packageName}".toUri()
            )
        )
    }

    /**
     * Queries the GitHub releases list.
     *
     * Not `releases/latest` or `releases/tags/<t>`: libbox's HTTP client throws on any non-200
     * (`Execute()` -> "HTTP 404 Not Found: {...}") and exposes no status code, so the preview
     * channel — which has no release most of the time — surfaced as a raw error toast. The list
     * endpoint always answers 200, and an empty channel is then just "nothing to show".
     */
    private fun checkUpdate(checkPreview: Boolean) {
        runOnIoDispatcher {
            try {
                val client = Libcore.newHttpClient().apply {
                    modernTLS()
                    trySocks5(DataStore.mixedPort)
                }
                val response = client.newRequest().apply {
                    setURL(RELEASES_API)
                }.execute()
                val releases = JSONArray(Util.getStringBox(response.contentString))
                val release = (0 until releases.length())
                    .map { releases.getJSONObject(it) }
                    .firstOrNull { candidate ->
                        if (candidate.optBoolean("draft")) return@firstOrNull false
                        val tag = candidate.optString("tag_name")
                        if (checkPreview) tag == PREVIEW_TAG
                        else !candidate.optBoolean("prerelease") && tag != PREVIEW_TAG
                    }
                // never getString(): a release may carry no `name`, and a missing channel has no
                // object at all
                val tag = release?.optString("tag_name").orEmpty()
                if (tag.isBlank()) {
                    runOnMainDispatcher {
                        Toast.makeText(
                            app,
                            if (checkPreview) R.string.check_update_no_preview else R.string.check_update_no,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return@runOnIoDispatcher
                }
                val expected = if (checkPreview) BuildConfig.PRE_VERSION_NAME else BuildConfig.VERSION_NAME
                val haveUpdate = !tag.contains(expected)
                val releaseUrl = release?.optString("html_url").orEmpty()
                runOnMainDispatcher {
                    if (haveUpdate) {
                        val context = requireContext()
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.update_dialog_title)
                            .setMessage(
                                context.getString(
                                    R.string.update_dialog_message,
                                    SagerNet.appVersionNameForDisplay,
                                    tag
                                )
                            )
                            .setPositiveButton(R.string.yes) { _, _ ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, releaseUrl.toUri()))
                            }
                            .setNegativeButton(R.string.no, null)
                            .show()
                    } else {
                        Toast.makeText(app, R.string.check_update_no, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Logs.w(e)
                runOnMainDispatcher {
                    Toast.makeText(app, e.readableMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
