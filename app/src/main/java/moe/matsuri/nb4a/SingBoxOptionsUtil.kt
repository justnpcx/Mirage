package moe.matsuri.nb4a

import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.RuleSetEntity
import com.justnpcx.mirage.fmt.TAG_PROXY
import moe.matsuri.nb4a.SingBoxOptions.RuleSet

object SingBoxOptionsUtil {

    fun domainStrategy(tag: String): String {
        fun auto2(key: String, newS: String): String {
            return (DataStore.configurationStore.getString(key) ?: "").replace("auto", newS)
        }
        return when (tag) {
            "dns-remote" -> {
                auto2("domain_strategy_for_remote", "")
            }

            "dns-direct" -> {
                auto2("domain_strategy_for_direct", "")
            }

            // server
            else -> {
                auto2("domain_strategy_for_server", "prefer_ipv4")
            }
        }
    }

}

/**
 * Resolve one domain token to a rule set tag, or null if it is an ordinary domain.
 *
 * `ruleset:foo` is an explicit reference and always wins. A bare `foo` counts as a reference only
 * when a rule set with that tag exists, so an ordinary domain is never mistaken for one. An
 * explicit reference to an undeclared tag is dropped rather than emitted: a dangling tag would make
 * sing-box refuse to start the whole config.
 */
private fun String.resolveRuleSetTag(remoteTags: Set<String>): String? {
    val explicit = startsWith("ruleset:")
    val tag = removePrefix("ruleset:")
    if (tag.isNotBlank() && tag in remoteTags) return tag
    return if (explicit) "" else null
}

/** Place one domain token: a built-in database, a declared rule set, or an ordinary pattern. */
private fun SingBoxOptions.Rule_DefaultOptions.placeDomainToken(token: String, remoteTags: Set<String>) {
    when {
        token.startsWith("geosite:") -> rule_set!!.plusAssign(token)
        token.startsWith("full:") -> domain!!.plusAssign(token.removePrefix("full:").lowercase())
        token.startsWith("domain:") -> domain_suffix!!.plusAssign(token.removePrefix("domain:").lowercase())
        token.startsWith("regexp:") -> domain_regex!!.plusAssign(token.removePrefix("regexp:").lowercase())
        token.startsWith("keyword:") -> domain_keyword!!.plusAssign(token.removePrefix("keyword:").lowercase())
        else -> when (val tag = token.resolveRuleSetTag(remoteTags)) {
            null -> domain_suffix!!.plusAssign(token.lowercase())
            "" -> Unit // explicit but undeclared -- skip
            else -> rule_set!!.plusAssign(tag)
        }
    }
}

private fun SingBoxOptions.DNSRule_DefaultOptions.placeDomainToken(token: String, remoteTags: Set<String>) {
    when {
        token.startsWith("geosite:") -> rule_set!!.plusAssign(token)
        token.startsWith("full:") -> domain!!.plusAssign(token.removePrefix("full:").lowercase())
        token.startsWith("domain:") -> domain_suffix!!.plusAssign(token.removePrefix("domain:").lowercase())
        token.startsWith("regexp:") -> domain_regex!!.plusAssign(token.removePrefix("regexp:").lowercase())
        token.startsWith("keyword:") -> domain_keyword!!.plusAssign(token.removePrefix("keyword:").lowercase())
        else -> when (val tag = token.resolveRuleSetTag(remoteTags)) {
            null -> domain_suffix!!.plusAssign(token.lowercase())
            "" -> Unit
            else -> rule_set!!.plusAssign(tag)
        }
    }
}

private fun SingBoxOptions.Rule_DefaultOptions.pruneEmptyRuleFields() {
    ip_cidr?.removeIf { it.isNullOrBlank() }
    rule_set?.removeIf { it.isNullOrBlank() }
    domain?.removeIf { it.isNullOrBlank() }
    domain_suffix?.removeIf { it.isNullOrBlank() }
    domain_regex?.removeIf { it.isNullOrBlank() }
    domain_keyword?.removeIf { it.isNullOrBlank() }
    if (ip_cidr?.isEmpty() == true) ip_cidr = null
    if (rule_set?.isEmpty() == true) rule_set = null
    if (domain?.isEmpty() == true) domain = null
    if (domain_suffix?.isEmpty() == true) domain_suffix = null
    if (domain_regex?.isEmpty() == true) domain_regex = null
    if (domain_keyword?.isEmpty() == true) domain_keyword = null
}

private fun SingBoxOptions.DNSRule_DefaultOptions.pruneEmptyRuleFields() {
    rule_set?.removeIf { it.isNullOrBlank() }
    domain?.removeIf { it.isNullOrBlank() }
    domain_suffix?.removeIf { it.isNullOrBlank() }
    domain_regex?.removeIf { it.isNullOrBlank() }
    domain_keyword?.removeIf { it.isNullOrBlank() }
    if (rule_set?.isEmpty() == true) rule_set = null
    if (domain?.isEmpty() == true) domain = null
    if (domain_suffix?.isEmpty() == true) domain_suffix = null
    if (domain_regex?.isEmpty() == true) domain_regex = null
    if (domain_keyword?.isEmpty() == true) domain_keyword = null
}

fun SingBoxOptions.DNSRule_DefaultOptions.makeSingBoxRule(
    list: List<String>,
    remoteTags: Set<String> = emptySet(),
) {
    rule_set = mutableListOf()
    domain = mutableListOf()
    domain_suffix = mutableListOf()
    domain_regex = mutableListOf()
    domain_keyword = mutableListOf()
    list.forEach { placeDomainToken(it, remoteTags) }
    pruneEmptyRuleFields()
}

fun SingBoxOptions.DNSRule_DefaultOptions.checkEmpty(): Boolean {
    if (rule_set?.isNotEmpty() == true) return false
    if (domain?.isNotEmpty() == true) return false
    if (domain_suffix?.isNotEmpty() == true) return false
    if (domain_regex?.isNotEmpty() == true) return false
    if (domain_keyword?.isNotEmpty() == true) return false
    if (user_id?.isNotEmpty() == true) return false
    return true
}

/**
 * Emit the definition for every rule set a rule references.
 *
 * `geoip:` / `geosite:` are the databases bundled with the app; anything else must be a tag the
 * user declared in [remote], which becomes a `remote` rule set that sing-box fetches itself.
 */
fun generateRuleSet(
    ruleSetString: List<String>,
    ruleSet: MutableList<RuleSet>,
    remote: Map<String, RuleSetEntity> = emptyMap(),
) {
    ruleSetString.forEach {
        when {
            it.startsWith("geoip:") || it.startsWith("geosite:") -> {
                ruleSet.add(RuleSet().apply {
                    type = "local"
                    tag = it
                    format = "binary"
                    path = it
                })
            }

            else -> remote[it]?.let { entity ->
                ruleSet.add(RuleSet().apply {
                    type = "remote"
                    tag = entity.tag
                    if (entity.url.isNotBlank()) url = entity.url
                    if (entity.format.isNotBlank()) format = entity.format
                    if (entity.updateInterval.isNotBlank()) update_interval = entity.updateInterval
                    // Always dial the download through the proxy. A remote rule set normally lives
                    // on a foreign host, and the two alternatives are both dead ends: leaving
                    // http_client off falls back to the default outbound implicitly, which sing-box
                    // 1.14 already reports as deprecated (removed in 1.16), and pointing the detour
                    // at our `bypass` outbound is rejected outright -- "detour to an empty direct
                    // outbound makes no sense".
                    http_client = SingBoxOptions.HTTPClientOptions().apply {
                        detour = TAG_PROXY
                    }
                })
            }
        }
    }
}

fun SingBoxOptions.Rule_DefaultOptions.makeSingBoxRule(
    list: List<String>,
    isIP: Boolean,
    remoteTags: Set<String> = emptySet(),
) {
    if (isIP) {
        ip_cidr = mutableListOf()
        rule_set = mutableListOf()
    } else {
        rule_set = mutableListOf()
        domain = mutableListOf()
        domain_suffix = mutableListOf()
        domain_regex = mutableListOf()
        domain_keyword = mutableListOf()
    }
    list.forEach {
        if (isIP) {
            when {
                it == "geoip:private" -> ip_is_private = true
                it.startsWith("geoip:") -> rule_set!!.plusAssign(it)
                else -> when (val tag = it.resolveRuleSetTag(remoteTags)) {
                    null -> ip_cidr!!.plusAssign(it)
                    "" -> Unit
                    else -> rule_set!!.plusAssign(tag)
                }
            }
            return@forEach
        }
        placeDomainToken(it, remoteTags)
    }
    pruneEmptyRuleFields()
}

fun SingBoxOptions.Rule_DefaultOptions.checkEmpty(): Boolean {
    if (ip_cidr?.isNotEmpty() == true) return false
    if (domain?.isNotEmpty() == true) return false
    if (rule_set?.isNotEmpty() == true) return false
    if (domain_suffix?.isNotEmpty() == true) return false
    if (domain_regex?.isNotEmpty() == true) return false
    if (domain_keyword?.isNotEmpty() == true) return false
    if (user_id?.isNotEmpty() == true) return false
    //
    if (port?.isNotEmpty() == true) return false
    if (port_range?.isNotEmpty() == true) return false
    if (source_ip_cidr?.isNotEmpty() == true) return false
    //
    if (!_hack_custom_config.isNullOrBlank()) return false
    return true
}
