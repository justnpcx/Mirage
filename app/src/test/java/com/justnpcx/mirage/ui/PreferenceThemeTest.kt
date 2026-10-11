package com.justnpcx.mirage.ui

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.justnpcx.mirage.R
import com.justnpcx.mirage.utils.Theme
import moe.matsuri.nb4a.ui.ColorPickerPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * B3 guard: the whole-app preference restyle is applied through a single `preferenceTheme`
 * overlay, so one missing style mapping would silently fall back to the stock rows on that
 * family of preferences, without any build error.
 *
 * This matters because MaterialComponents resolves `BasePreferenceThemeOverlay`, which maps each
 * preference family to its own `Preference.*.Material` style — and each of those pins
 * `android:layout`. Overriding only `preferenceStyle` restyles plain preferences and leaves
 * switches (42 in the app), edit texts (119) and drop-downs (40) stock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PreferenceThemeTest {

    private fun base(): Context = ContextThemeWrapper(
        RuntimeEnvironment.getApplication(),
        R.style.Theme_SagerNet,
    )

    /** Mimics PreferenceFragmentCompat, which wraps the context in the preferenceTheme overlay. */
    private fun styled(): Context =
        ContextThemeWrapper(base(), R.style.PreferenceThemeOverlay_Mirage)

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
            preferenceThemeResId(base()),
        )
    }

    @Test
    fun colourVariantsInheritTheMirageOverlay() {
        // Theme.SagerNet.Red and friends declare no explicit parent, so they must pick up
        // preferenceTheme from Theme.SagerNet through dot-notation inheritance.
        assertEquals(
            R.style.PreferenceThemeOverlay_Mirage,
            preferenceThemeResId(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SagerNet_Red)),
        )
        assertEquals(
            R.style.PreferenceThemeOverlay_Mirage,
            preferenceThemeResId(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SagerNet_Black)),
        )
    }

    @Test
    fun everyPreferenceFamilyUsesAMirageRow() {
        val context = styled()
        assertEquals(R.layout.preference_mirage, Preference(context).layoutResource)
        assertEquals(R.layout.preference_mirage, SwitchPreference(context).layoutResource)
        assertEquals(R.layout.preference_mirage, SwitchPreferenceCompat(context).layoutResource)
        assertEquals(R.layout.preference_mirage, CheckBoxPreference(context).layoutResource)
        assertEquals(R.layout.preference_mirage, EditTextPreference(context).layoutResource)
        assertEquals(R.layout.preference_mirage, ListPreference(context).layoutResource)
        // the theme-colour row is a ColorPickerPreference, which borrows editTextPreferenceStyle
        assertEquals(R.layout.preference_mirage, ColorPickerPreference(context).layoutResource)
        assertEquals(R.layout.preference_category_mirage, PreferenceCategory(context).layoutResource)
    }

    @Test
    fun dropDownRowsKeepTheSpinnerThePreferenceLooksUp() {
        val context = styled()
        val preference = DropDownPreference(context)
        assertEquals(R.layout.preference_mirage_dropdown, preference.layoutResource)
        // SimpleMenuPreference.onBindViewHolder does findViewById(R.id.spinner) and dereferences
        // it, so the id must survive in the Mirage layout.
        val row = android.view.LayoutInflater.from(context)
            .inflate(preference.layoutResource, null, false)
        assertTrue(
            "Mirage drop-down row lost R.id.spinner",
            row.findViewById<android.view.View>(R.id.spinner) != null,
        )
    }

    @Test
    fun anUnsetThemeGetsTheMiragePalette() {
        // Theme.PINK_SSR (the old default) points colourPrimary at @color/color_pink_ssr
        // (#fb7299, NekoBox pink), which is why the app still looked like the original. The
        // unset default must land on Theme.SagerNet, whose ramp is the Mirage indigo.
        assertEquals(R.style.Theme_SagerNet, Theme.getTheme(0))
        assertEquals(R.style.Theme_SagerNet_Dialog, Theme.getDialogTheme(0))
    }
}
