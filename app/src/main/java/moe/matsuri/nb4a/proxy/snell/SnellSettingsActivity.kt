package moe.matsuri.nb4a.proxy.snell

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.justnpcx.mirage.Key
import com.justnpcx.mirage.R
import com.justnpcx.mirage.database.preference.EditTextPreferenceModifiers
import com.justnpcx.mirage.ktx.applyDefaultValues
import com.justnpcx.mirage.ui.profile.ProfileSettingsActivity
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class SnellSettingsActivity : ProfileSettingsActivity<SnellBean>() {
    override fun createEntity() = SnellBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val version = pbm.add(PreferenceBinding(Type.TextToInt, "version"))
    private val psk = pbm.add(PreferenceBinding(Type.Text, "psk"))
    private val userKey = pbm.add(PreferenceBinding(Type.Text, "userKey"))
    private val reuse = pbm.add(PreferenceBinding(Type.Bool, "reuse"))
    private val network = pbm.add(PreferenceBinding(Type.Text, "network"))
    private val obfsMode = pbm.add(PreferenceBinding(Type.Text, "obfsMode"))
    private val obfsHost = pbm.add(PreferenceBinding(Type.Text, "obfsHost"))
    private val mode = pbm.add(PreferenceBinding(Type.Text, "mode"))

    override fun SnellBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun SnellBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.snell_preferences)

        findPreference<EditTextPreference>(Key.SERVER_PORT)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Port)
        }
        findPreference<EditTextPreference>("psk")!!.apply {
            summaryProvider = PasswordSummaryProvider
        }

        // v4 takes obfs_*, v6 takes mode. Show only the set that matches the chosen version.
        val versionPref = findPreference<EditTextPreference>("version")!!
        val obfsModePref = findPreference<EditTextPreference>("obfsMode")!!
        val obfsHostPref = findPreference<EditTextPreference>("obfsHost")!!
        val modePref = findPreference<EditTextPreference>("mode")!!
        fun updateVersion(v: Int) {
            val v4 = v != 6
            obfsModePref.isVisible = v4
            obfsHostPref.isVisible = v4
            modePref.isVisible = !v4
        }
        updateVersion(versionPref.text?.toIntOrNull() ?: 4)
        versionPref.setOnPreferenceChangeListener { _, newValue ->
            updateVersion((newValue as String).toIntOrNull() ?: 4)
            true
        }
    }
}
