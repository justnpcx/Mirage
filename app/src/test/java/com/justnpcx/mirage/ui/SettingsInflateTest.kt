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
import moe.matsuri.nb4a.ui.ColorPickerPreference
import moe.matsuri.nb4a.ui.EditConfigPreference
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards the Settings screen against a crash on open.
 *
 * SettingsPreferenceFragment does `findPreference<...>(key)!!` for a long list of keys, so a single
 * preference that fails to inflate under the Mirage preferenceTheme turns into an NPE the moment
 * the screen is shown — and because CrashHandler restarts the app and pops a log-share sheet, the
 * user sees "share sheet" rather than an obvious crash.
 *
 * Robolectric builds the same styled context PreferenceFragmentCompat does (the activity theme
 * wrapped in the preferenceTheme overlay), then inflates the real XML.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsInflateTest {

    private fun styled(): Context = ContextThemeWrapper(
        ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SagerNet),
        R.style.PreferenceThemeOverlay_Mirage,
    )

    @Test
    fun globalPreferencesInflatesWithEveryKeyTheFragmentRequires() {
        val context = styled()
        val manager = PreferenceManager(context)
        val screen = manager.createPreferenceScreen(context)
        manager.inflateFromResource(context, R.xml.global_preferences, screen)

        // keys SettingsPreferenceFragment resolves with !! — a null here is the crash
        val required = listOf(
            "appTheme", "nightTheme", "appLanguage", "serviceMode", "tunImplementation",
            "mixedPort", "appendHttpProxy", "showDirectSpeed", "ipv6Mode", "trafficSniffing",
            "bypassLan", "bypassLanInCore", "remoteDns", "directDns", "enableDnsRouting",
            "enableFakeDns", "logLevel", "mtu", "globalCustomConfig", "meteredNetwork",
            "proxyApps", "profileTrafficStatistics", "speedInterval", "resolveDestination",
            "acquireWakeLock", "enableClashAPI", "allowAccess",
        )
        val missing = required.filter { screen.findPreference<Preference>(it) == null }
        assertTrue("preferences missing after inflation: $missing", missing.isEmpty())
    }

    /**
     * Binds every row the way PreferenceGroupAdapter does. A crash here is what the user sees as
     * "tap Settings and it jumps to a share sheet", because CrashHandler restarts the app and pops
     * a log share.
     *
     * Two rows are skipped because their binding genuinely cannot run on the JVM: both
     * ColorPickerPreference and EditConfigPreference end up in com.justnpcx.mirage.ktx, whose
     * <clinit> reflects on Socket.getFileDescriptor$() (fine on a device, NoSuchMethodException on
     * the JVM). They are still covered for inflation by the test above.
     */
    @Test
    fun everySettingsRowBinds() {
        val context = styled()
        val manager = PreferenceManager(context)
        val screen = manager.createPreferenceScreen(context)
        manager.inflateFromResource(context, R.xml.global_preferences, screen)

        val failures = mutableListOf<String>()
        fun bind(preference: Preference) {
            if (preference is ColorPickerPreference || preference is EditConfigPreference) return
            try {
                val view = LayoutInflater.from(context).inflate(preference.layoutResource, null, false)
                val frame = view.findViewById<ViewGroup?>(android.R.id.widget_frame)
                val widgetRes = Preference::class.java
                    .getDeclaredField("mWidgetLayoutResId")
                    .apply { isAccessible = true }.getInt(preference)
                if (frame != null && widgetRes != 0 && frame.childCount == 0) {
                    LayoutInflater.from(context).inflate(widgetRes, frame)
                }
                preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(view))
            } catch (t: Throwable) {
                failures += "${preference.key} (${preference.javaClass.simpleName}): $t"
            }
        }
        fun walk(group: PreferenceGroup) {
            for (i in 0 until group.preferenceCount) {
                val child = group.getPreference(i)
                if (child is PreferenceGroup) walk(child) else bind(child)
            }
        }
        walk(screen)

        assertTrue("rows that failed to bind:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
