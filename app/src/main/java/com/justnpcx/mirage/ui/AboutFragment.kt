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
import io.nekohasekai.sagernet.plugin.PluginManager.loadString
import com.justnpcx.mirage.ui.compose.AboutScreen
import com.justnpcx.mirage.ui.compose.AboutState
import com.justnpcx.mirage.ui.compose.PluginInfo
import com.justnpcx.mirage.ui.compose.theme.MirageTheme
import com.justnpcx.mirage.utils.PackageCache
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins
import moe.matsuri.nb4a.utils.Util
import org.json.JSONObject

/**
 * About screen — Compose implementation (replaces the old `material-about-library` screen).
 */
class AboutFragment : ToolbarFragment(R.layout.layout_compose) {

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
            val license = runCatching {
                requireContext().assets.open("LICENSE").bufferedReader().readText()
            }.getOrDefault("")

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
                    license = license,
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

    private fun checkUpdate(checkPreview: Boolean) {
        runOnIoDispatcher {
            try {
                val client = Libcore.newHttpClient().apply {
                    modernTLS()
                    trySocks5(DataStore.mixedPort)
                }
                val response = client.newRequest().apply {
                    if (checkPreview) {
                        setURL("https://api.github.com/repos/justnpcx/Mirage/releases/tags/preview")
                    } else {
                        setURL("https://api.github.com/repos/justnpcx/Mirage/releases/latest")
                    }
                }.execute()
                val release = JSONObject(Util.getStringBox(response.contentString))
                val releaseName = release.getString("name")
                val releaseUrl = release.getString("html_url")
                var haveUpdate = releaseName.isNotBlank()
                haveUpdate = if (isPreview) {
                    if (checkPreview) {
                        haveUpdate && releaseName != BuildConfig.PRE_VERSION_NAME
                    } else {
                        haveUpdate && releaseName != BuildConfig.VERSION_NAME
                    }
                } else {
                    haveUpdate && !releaseName.contains(BuildConfig.VERSION_NAME)
                }
                runOnMainDispatcher {
                    if (haveUpdate) {
                        val context = requireContext()
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.update_dialog_title)
                            .setMessage(
                                context.getString(
                                    R.string.update_dialog_message,
                                    SagerNet.appVersionNameForDisplay,
                                    releaseName
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
