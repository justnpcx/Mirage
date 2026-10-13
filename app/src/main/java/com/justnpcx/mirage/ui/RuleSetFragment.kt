package com.justnpcx.mirage.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.justnpcx.mirage.R
import com.justnpcx.mirage.database.RuleSetEntity
import com.justnpcx.mirage.database.SagerDatabase
import com.justnpcx.mirage.databinding.LayoutEmptyRuleSetBinding
import com.justnpcx.mirage.databinding.LayoutRuleSetItemBinding
import com.justnpcx.mirage.ktx.*
import com.justnpcx.mirage.widget.ListListener

/**
 * Manages the remote rule sets the config subscribes to. A row is only a tag plus a url; sing-box
 * downloads and refreshes the file itself, so there is nothing to fetch here.
 */
class RuleSetFragment : ToolbarFragment(R.layout.layout_rule_set), Toolbar.OnMenuItemClickListener {

    lateinit var activity: MainActivity
    lateinit var ruleSetListView: RecyclerView
    lateinit var ruleSetAdapter: RuleSetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as MainActivity

        ViewCompat.setOnApplyWindowInsetsListener(view, ListListener)
        toolbar.setTitle(R.string.rule_sets)
        toolbar.inflateMenu(R.menu.add_rule_set_menu)
        toolbar.setOnMenuItemClickListener(this)

        ruleSetListView = view.findViewById(R.id.rule_set_list)
        ruleSetListView.layoutManager = FixedLinearLayoutManager(ruleSetListView)
        ruleSetAdapter = RuleSetAdapter()
        ruleSetListView.adapter = ruleSetAdapter
    }

    override fun onResume() {
        super.onResume()
        if (::ruleSetAdapter.isInitialized) {
            runOnDefaultDispatcher { ruleSetAdapter.reload() }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_rule_set -> {
                startActivity(Intent(context, RuleSetSettingsActivity::class.java))
            }

            R.id.action_reset_rule_set -> {
                MaterialAlertDialogBuilder(activity).setTitle(R.string.confirm)
                    .setMessage(R.string.rule_set_delete_prompt)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        runOnDefaultDispatcher {
                            SagerDatabase.ruleSetDao.reset()
                            ruleSetAdapter.reload()
                        }
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }
        }
        return true
    }

    inner class RuleSetAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val ruleSetList = ArrayList<RuleSetEntity>()

        suspend fun reload() {
            val ruleSets = SagerDatabase.ruleSetDao.allRuleSets()
            ruleSetListView.post {
                ruleSetList.clear()
                ruleSetList.addAll(ruleSets)
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                EmptyHolder(LayoutEmptyRuleSetBinding.inflate(layoutInflater, parent, false))
            } else {
                RuleSetHolder(LayoutRuleSetItemBinding.inflate(layoutInflater, parent, false))
            }
        }

        // The hint card is a placeholder, not a permanent row: showing "No rule sets yet" above a
        // list of rule sets reads as a contradiction.
        override fun getItemViewType(position: Int) = if (ruleSetList.isEmpty()) 0 else 1

        override fun getItemCount() = if (ruleSetList.isEmpty()) 1 else ruleSetList.size

        override fun getItemId(position: Int): Long {
            if (ruleSetList.isEmpty()) return 0L
            return ruleSetList[position].id
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is RuleSetHolder) holder.bind(ruleSetList[position])
        }

        inner class EmptyHolder(binding: LayoutEmptyRuleSetBinding) : RecyclerView.ViewHolder(binding.root)

        inner class RuleSetHolder(binding: LayoutRuleSetItemBinding) : RecyclerView.ViewHolder(binding.root) {

            lateinit var ruleSet: RuleSetEntity
            val name = binding.ruleSetName
            val summary = binding.ruleSetSummary
            val editButton = binding.edit
            val enableSwitch = binding.enable

            fun bind(entity: RuleSetEntity) {
                ruleSet = entity
                name.text = ruleSet.displayName()
                summary.text = ruleSet.mkSummary()
                itemView.setOnClickListener { enableSwitch.performClick() }
                enableSwitch.isChecked = ruleSet.enabled
                enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                    runOnDefaultDispatcher {
                        ruleSet.enabled = isChecked
                        SagerDatabase.ruleSetDao.updateRuleSet(ruleSet)
                        onMainDispatcher { reload() }
                    }
                }
                editButton.setOnClickListener {
                    startActivity(Intent(it.context, RuleSetSettingsActivity::class.java).apply {
                        putExtra(RuleSetSettingsActivity.EXTRA_RULE_SET_ID, ruleSet.id)
                    })
                }
            }
        }
    }
}
