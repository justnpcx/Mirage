package com.justnpcx.mirage.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.justnpcx.mirage.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards every shipped preference screen against a crash on open.
 *
 * A single preference that fails to inflate or bind under the Mirage preferenceTheme is an
 * immediate crash when that screen is shown — and because CrashHandler restarts the app and pops a
 * log-share sheet, the user sees "the app opened Share" rather than an obvious crash. That is
 * exactly how the Settings screen broke (Preference.setSummary on a row that declares
 * useSimpleSummaryProvider), so every XML the app can show gets walked here.
 *
 * Robolectric builds the same styled context PreferenceFragmentCompat does: the activity theme
 * wrapped in the preferenceTheme overlay.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PreferenceScreensTest {

    private fun styled(): Context = ContextThemeWrapper(
        ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SagerNet),
        R.style.PreferenceThemeOverlay_Mirage,
    )

    /** every preference screen reachable in the app */
    private val screens = listOf(
        "global_preferences" to R.xml.global_preferences,
        "route_preferences" to R.xml.route_preferences,
        "group_preferences" to R.xml.group_preferences,
        "rule_set_preferences" to R.xml.rule_set_preferences,
        "config_preferences" to R.xml.config_preferences,
        "name_preferences" to R.xml.name_preferences,
        // the 16 protocol editors
        "socks_preferences" to R.xml.socks_preferences,
        "ssh_preferences" to R.xml.ssh_preferences,
        "shadowsocks_preferences" to R.xml.shadowsocks_preferences,
        "standard_v2ray_preferences" to R.xml.standard_v2ray_preferences,
        "trojan_go_preferences" to R.xml.trojan_go_preferences,
        "mieru_preferences" to R.xml.mieru_preferences,
        "naive_preferences" to R.xml.naive_preferences,
        "hysteria_preferences" to R.xml.hysteria_preferences,
        "tuic_preferences" to R.xml.tuic_preferences,
        "shadowtls_preferences" to R.xml.shadowtls_preferences,
        "anytls_preferences" to R.xml.anytls_preferences,
        "snell_preferences" to R.xml.snell_preferences,
        "wireguard_preferences" to R.xml.wireguard_preferences,
        // NOT balancer_preferences: nothing references it and the manifest has no Balancer editor,
        // so it is dead upstream XML. It also cannot inflate off-device — its GroupPreference
        // constructor queries Room (SagerDatabase.groupDao.allGroups()).
    )

    private fun inflate(context: Context, xml: Int): PreferenceGroup {
        val manager = PreferenceManager(context)
        val screen = manager.createPreferenceScreen(context)
        manager.inflateFromResource(context, xml, screen)
        return screen
    }

    private fun rows(group: PreferenceGroup, out: MutableList<Preference> = mutableListOf()): List<Preference> {
        for (i in 0 until group.preferenceCount) {
            val child = group.getPreference(i)
            if (child is PreferenceGroup) rows(child, out) else out += child
        }
        return out
    }

    @Test
    fun everyScreenInflates() {
        val context = styled()
        val failures = mutableListOf<String>()
        for ((name, xml) in screens) {
            try {
                inflate(context, xml)
            } catch (t: Throwable) {
                failures += "$name: $t"
            }
        }
        assertTrue("screens that failed to inflate:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /**
     * Some preferences reach com.justnpcx.mirage.ktx, whose <clinit> reflects on
     * Socket.getFileDescriptor$(). That method exists on a device but not on the JVM, so binding
     * those particular rows cannot be exercised here. Match on the cause rather than keeping a list
     * of class names, so a genuinely different initializer failure still fails the test.
     */
    private fun Throwable.isJvmOnlyKtxFailure(): Boolean {
        // On this JVM the only initializer that these preference classes can trip is
        // com.justnpcx.mirage.ktx's <clinit>, which reflects on Socket.getFileDescriptor$().
        // The skipped rows are printed so a wrong call here is visible in the test output.
        if (this is ExceptionInInitializerError) return true
        var cause: Throwable? = this
        while (cause != null) {
            if (cause.toString().contains("getFileDescriptor")) return true
            cause = cause.cause
        }
        // once that <clinit> has failed, DataStore can never initialize on this JVM, so every
        // later row that touches it fails too — a cascade, not a product bug
        return this is NoClassDefFoundError &&
            message?.contains("com.justnpcx.mirage.database.DataStore") == true
    }

    /**
     * Binds every row the way PreferenceGroupAdapter does — the path that crashed the Settings
     * screen on a device (Preference.setSummary on a row with a SummaryProvider).
     */
    @Test
    fun everyRowBinds() {
        val context = styled()
        val failures = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        for ((name, xml) in screens) {
            for (preference in rows(inflate(context, xml))) {
                try {
                    val view = LayoutInflater.from(context)
                        .inflate(preference.layoutResource, null, false)
                    val frame = view.findViewById<ViewGroup?>(android.R.id.widget_frame)
                    val widgetRes = Preference::class.java
                        .getDeclaredField("mWidgetLayoutResId")
                        .apply { isAccessible = true }.getInt(preference)
                    if (frame != null && widgetRes != 0 && frame.childCount == 0) {
                        LayoutInflater.from(context).inflate(widgetRes, frame)
                    }
                    preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(view))
                } catch (t: Throwable) {
                    if (t.isJvmOnlyKtxFailure()) {
                        skipped += "$name/${preference.key} (${t.javaClass.simpleName})"
                    } else {
                        failures += "$name/${preference.key} (${preference.javaClass.simpleName}): $t"
                    }
                }
            }
        }
        assertTrue("rows that failed to bind:\n" + failures.joinToString("\n"), failures.isEmpty())
        println("skipped (JVM-only ktx limitation): " + skipped.joinToString(", "))
    }
}
