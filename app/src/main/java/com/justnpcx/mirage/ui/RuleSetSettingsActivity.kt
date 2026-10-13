package com.justnpcx.mirage.ui

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.github.shadowsocks.plugin.Empty
import com.github.shadowsocks.plugin.fragment.AlertDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.justnpcx.mirage.Key
import com.justnpcx.mirage.R
import com.justnpcx.mirage.SagerNet
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.RuleSetEntity
import com.justnpcx.mirage.database.SagerDatabase
import com.justnpcx.mirage.database.preference.OnPreferenceDataStoreChangeListener
import com.justnpcx.mirage.ktx.Logs
import com.justnpcx.mirage.ktx.onMainDispatcher
import com.justnpcx.mirage.ktx.runOnDefaultDispatcher
import com.justnpcx.mirage.widget.ListListener
import kotlinx.parcelize.Parcelize

/**
 * Editor for a single remote rule set. Mirrors [RouteSettingsActivity]: the fields live in
 * DataStore while editing and are written back on apply.
 */
@Suppress("UNCHECKED_CAST")
class RuleSetSettingsActivity(
    @LayoutRes resId: Int = R.layout.layout_settings_activity,
) : ThemedActivity(resId),
    OnPreferenceDataStoreChangeListener {

    fun RuleSetEntity.init() {
        DataStore.ruleSetTag = tag
        DataStore.ruleSetUrl = url
        DataStore.ruleSetFormat = format
        DataStore.ruleSetUpdateInterval = updateInterval
    }

    fun RuleSetEntity.serialize() {
        tag = DataStore.ruleSetTag.trim()
        url = DataStore.ruleSetUrl.trim()
        format = DataStore.ruleSetFormat
        updateInterval = DataStore.ruleSetUpdateInterval.trim()

        if (DataStore.editingId == 0L) {
            enabled = true
        }
    }

    fun needSave(): Boolean {
        return DataStore.dirty
    }

    fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.rule_set_preferences)
    }

    class UnsavedChangesDialogFragment : AlertDialogFragment<Empty, Empty>() {
        override fun AlertDialog.Builder.prepare(listener: DialogInterface.OnClickListener) {
            setTitle(R.string.unsaved_changes_prompt)
            setPositiveButton(R.string.yes) { _, _ ->
                runOnDefaultDispatcher {
                    (requireActivity() as RuleSetSettingsActivity).saveAndExit()
                }
            }
            setNegativeButton(R.string.no) { _, _ ->
                requireActivity().finish()
            }
            setNeutralButton(android.R.string.cancel, null)
        }
    }

    @Parcelize
    data class RuleSetIdArg(val ruleSetId: Long) : Parcelable
    class DeleteConfirmationDialogFragment : AlertDialogFragment<RuleSetIdArg, Empty>() {
        override fun AlertDialog.Builder.prepare(listener: DialogInterface.OnClickListener) {
            setTitle(R.string.rule_set_delete_prompt)
            setPositiveButton(R.string.yes) { _, _ ->
                runOnDefaultDispatcher {
                    SagerDatabase.ruleSetDao.getById(arg.ruleSetId)?.let {
                        SagerDatabase.ruleSetDao.deleteRuleSet(it)
                    }
                }
                requireActivity().finish()
            }
            setNegativeButton(R.string.no, null)
        }
    }

    companion object {
        const val EXTRA_RULE_SET_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.rule_set_settings)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }

        if (savedInstanceState == null) {
            val editingId = intent.getLongExtra(EXTRA_RULE_SET_ID, 0L)
            DataStore.editingId = editingId
            runOnDefaultDispatcher {
                if (editingId == 0L) {
                    // Reset the scratch fields. Without this a new rule set inherits whatever the
                    // previous edit left in the shared DataStore.
                    RuleSetEntity().init()
                } else {
                    val entity = SagerDatabase.ruleSetDao.getById(editingId)
                    if (entity == null) {
                        onMainDispatcher { finish() }
                        return@runOnDefaultDispatcher
                    }
                    entity.init()
                }

                onMainDispatcher {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.settings, MyPreferenceFragmentCompat())
                        .commit()

                    DataStore.dirty = false
                    DataStore.profileCacheStore.registerChangeListener(this@RuleSetSettingsActivity)
                }
            }
        }
    }

    private suspend fun showError(message: Int) {
        onMainDispatcher {
            MaterialAlertDialogBuilder(this@RuleSetSettingsActivity)
                .setTitle(R.string.rule_set_settings)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    suspend fun saveAndExit() {
        val editingId = DataStore.editingId
        if (editingId == 0L || needSave()) {
            val entity = if (editingId == 0L) {
                RuleSetEntity()
            } else {
                SagerDatabase.ruleSetDao.getById(editingId) ?: run {
                    finish()
                    return
                }
            }
            entity.serialize()

            if (entity.tag.isBlank()) {
                showError(R.string.rule_set_empty_tag)
                return
            }
            if (entity.url.isBlank()) {
                showError(R.string.rule_set_url)
                return
            }
            // sing-box refuses to start on duplicate rule set tags, and a tag that looks like a
            // built-in database would shadow it.
            val clash = SagerDatabase.ruleSetDao.allRuleSets().any {
                it.id != entity.id && it.tag == entity.tag
            } || entity.tag.startsWith("geosite:") || entity.tag.startsWith("geoip:")
            if (clash) {
                showError(R.string.rule_set_duplicate_tag)
                return
            }

            if (editingId == 0L) {
                entity.userOrder = SagerDatabase.ruleSetDao.nextOrder() ?: 1
                SagerDatabase.ruleSetDao.createRuleSet(entity)
            } else {
                SagerDatabase.ruleSetDao.updateRuleSet(entity)
            }
        }
        finish()
    }

    val child by lazy { supportFragmentManager.findFragmentById(R.id.settings) as MyPreferenceFragmentCompat }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_config_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = child.onOptionsItemSelected(item)

    override fun onBackPressed() {
        if (needSave()) {
            UnsavedChangesDialogFragment().apply { key() }.show(supportFragmentManager, null)
        } else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) finish()
        return true
    }

    override fun onDestroy() {
        DataStore.profileCacheStore.unregisterChangeListener(this)
        super.onDestroy()
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        if (key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    class MyPreferenceFragmentCompat : PreferenceFragmentCompat() {

        var activity: RuleSetSettingsActivity? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = DataStore.profileCacheStore
            try {
                activity = (requireActivity() as RuleSetSettingsActivity).apply {
                    createPreferences(savedInstanceState, rootKey)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    SagerNet.application,
                    "Error on createPreferences, please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                Logs.e(e)
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            ViewCompat.setOnApplyWindowInsetsListener(listView, ListListener)
        }

        override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.action_delete -> {
                if (DataStore.editingId == 0L) {
                    requireActivity().finish()
                } else {
                    DeleteConfirmationDialogFragment().apply {
                        arg(RuleSetIdArg(DataStore.editingId))
                        key()
                    }.show(parentFragmentManager, null)
                }
                true
            }

            R.id.action_apply -> {
                runOnDefaultDispatcher {
                    activity?.saveAndExit()
                }
                true
            }

            else -> false
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            super.onDisplayPreferenceDialog(preference)
        }

    }

}
