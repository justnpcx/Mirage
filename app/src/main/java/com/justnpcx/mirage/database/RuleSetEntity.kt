package com.justnpcx.mirage.database

import androidx.room.*

/**
 * A rule set the user subscribes to.
 *
 * The built-in `geosite:` / `geoip:` databases ship with the app and need no row here. These are
 * the remote ones: sing-box downloads them itself and refreshes them on [updateInterval], so the
 * app only has to hand over the url and a tag that route rules can reference.
 */
@Entity(tableName = "rule_sets")
data class RuleSetEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var userOrder: Long = 0L,
    var tag: String = "",
    var url: String = "",
    /** Empty lets sing-box infer from the url extension (`.srs` -> binary, `.json` -> source). */
    @ColumnInfo(defaultValue = "") var format: String = "",
    /** A duration string such as "1d". Empty keeps the sing-box default. */
    @ColumnInfo(defaultValue = "") var updateInterval: String = "",
    var enabled: Boolean = false,
) {

    fun displayName(): String {
        return tag.ifBlank { "Rule set $id" }
    }

    fun mkSummary(): String {
        val parts = mutableListOf<String>()
        if (url.isNotBlank()) parts.add(url)
        if (format.isNotBlank()) parts.add(format)
        if (updateInterval.isNotBlank()) parts.add(updateInterval)
        return parts.joinToString(" · ")
    }

    @androidx.room.Dao
    interface Dao {

        @Query("SELECT * FROM rule_sets ORDER BY userOrder")
        fun allRuleSets(): List<RuleSetEntity>

        @Query("SELECT * FROM rule_sets WHERE enabled = :enabled ORDER BY userOrder")
        fun enabledRuleSets(enabled: Boolean = true): List<RuleSetEntity>

        @Query("SELECT * FROM rule_sets WHERE id = :id")
        fun getById(id: Long): RuleSetEntity?

        @Query("SELECT MAX(userOrder) + 1 FROM rule_sets")
        fun nextOrder(): Long?

        @Delete
        fun deleteRuleSet(ruleSet: RuleSetEntity)

        @Insert
        fun createRuleSet(ruleSet: RuleSetEntity): Long

        @Update
        fun updateRuleSet(ruleSet: RuleSetEntity)

        @Query("DELETE FROM rule_sets")
        fun reset()

    }
}
