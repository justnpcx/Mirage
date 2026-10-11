package com.justnpcx.mirage.ui

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.justnpcx.mirage.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * B3 guard: the whole-app preference restyle is applied through a single `preferenceTheme`
 * overlay, so one broken style attribute would silently fall back to the stock rows on every
 * screen (settings, route, and all 16 protocol editors) without any build error.
 *
 * These assertions pin that wiring on the JVM instead of relying on a visual check on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PreferenceThemeTest {

    private fun themed(style: Int): Context =
        ContextThemeWrapper(RuntimeEnvironment.getApplication(), style)

    private fun preferenceThemeResId(context: Context): Int {
        val tv = TypedValue()
        assertTrue(
            "theme does not define preferenceTheme",
            context.theme.resolveAttribute(R.attr.preferenceTheme, tv, true),
        )
        return tv.resourceId
    }

    @Test
    fun mainThemePointsAtTheMiragePreferenceOverlay() {
        assertEquals(
            R.style.PreferenceThemeOverlay_Mirage,
            preferenceThemeResId(themed(R.style.Theme_SagerNet)),
        )
    }

    @Test
    fun colourVariantsInheritTheMirageOverlay() {
        // Theme.SagerNet.Red and friends declare no explicit parent, so they must pick up
        // preferenceTheme from Theme.SagerNet through dot-notation inheritance.
        assertEquals(
            R.style.PreferenceThemeOverlay_Mirage,
            preferenceThemeResId(themed(R.style.Theme_SagerNet_Red)),
        )
        assertEquals(
            R.style.PreferenceThemeOverlay_Mirage,
            preferenceThemeResId(themed(R.style.Theme_SagerNet_Black)),
        )
    }

    @Test
    fun preferencesResolveToTheRoundedMirageRows() {
        // PreferenceFragmentCompat wraps the activity context in the preferenceTheme overlay
        // (mStyledContext) before building preferences — mimic that here.
        val context = ContextThemeWrapper(
            themed(R.style.Theme_SagerNet),
            R.style.PreferenceThemeOverlay_Mirage,
        )
        assertEquals(R.layout.preference_mirage, Preference(context).layoutResource)
        assertEquals(R.layout.preference_mirage, SwitchPreferenceCompat(context).layoutResource)
        assertEquals(R.layout.preference_category_mirage, PreferenceCategory(context).layoutResource)
    }
}
