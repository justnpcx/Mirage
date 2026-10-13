package com.justnpcx.mirage.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.justnpcx.mirage.R
import com.justnpcx.mirage.ktx.*
import com.justnpcx.mirage.ui.compose.LogcatScreen
import com.justnpcx.mirage.ui.compose.LogcatState
import com.justnpcx.mirage.ui.compose.parseLog
import com.justnpcx.mirage.ui.compose.theme.MirageTheme
import libcore.Libcore
import moe.matsuri.nb4a.utils.SendLog

/**
 * Kernel log — Compose implementation.
 *
 * The old version built one giant SpannableString and re-laid it out on every refresh, which is
 * why a long log was slow to show. A LazyColumn only lays out what is on screen.
 */
class LogcatFragment : ToolbarFragment(R.layout.layout_compose),
    Toolbar.OnMenuItemClickListener {

    private var state by mutableStateOf(LogcatState())

    /** The diagnosis dialog is shown once per log session, not on every re-bind. */
    private var diagnosed = false

    /**
     * Matches the failures this build knows how to explain, in the order that reflects causation:
     * a dead proxy also produces DNS and timeout errors, so those are checked after it.
     */
    private fun diagnose(log: String): Pair<Int, Int>? = when {
        "reality verification failed" in log ||
            "received real certificate" in log -> R.string.diag_reality_title to R.string.diag_reality_msg

        "dns: exchange failed" in log -> R.string.diag_dns_title to R.string.diag_dns_msg

        "x509" in log || "certificate verify" in log || "tls: failed to verify" in log ->
            R.string.diag_cert_title to R.string.diag_cert_msg

        "i/o timeout" in log || "connection timeout" in log ->
            R.string.diag_timeout_title to R.string.diag_timeout_msg

        else -> null
    }

    /**
     * The kernel's real error is otherwise buried in a wall of trace lines, and it is easy to
     * misread a downstream symptom (DNS, timeouts) as the cause. Surface it instead.
     */
    private fun maybeDiagnose(log: String) {
        if (diagnosed) return
        val (title, message) = diagnose(log) ?: return
        diagnosed = true
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.diag_dismiss, null)
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_log)
        toolbar.inflateMenu(R.menu.logcat_menu)
        toolbar.setOnMenuItemClickListener(this)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MirageTheme {
                LogcatScreen(state = state)
            }
        }
        reloadSession()
    }

    private fun reloadSession() {
        val raw = String(SendLog.getNekoLog(50 * 1024))
        state = LogcatState(parseLog(raw))
        maybeDiagnose(raw)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_logcat -> {
                runOnDefaultDispatcher {
                    try {
                        Libcore.nekoLogClear()
                        Runtime.getRuntime().exec("/system/bin/logcat -c")
                    } catch (e: Exception) {
                        onMainDispatcher {
                            snackbar(e.readableMessage).show()
                        }
                        return@runOnDefaultDispatcher
                    }
                    onMainDispatcher {
                        state = LogcatState()
                        // a cleared log starts a new session, so a fresh problem may be diagnosed
                        diagnosed = false
                    }
                }
            }

            R.id.action_send_logcat -> {
                val context = requireContext()
                runOnDefaultDispatcher {
                    SendLog.sendLog(context, "NB4A")
                }
            }

            R.id.action_refresh -> {
                reloadSession()
            }
        }
        return true
    }

}
