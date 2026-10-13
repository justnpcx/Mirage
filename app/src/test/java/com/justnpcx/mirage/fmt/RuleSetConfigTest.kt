package com.justnpcx.mirage.fmt

import com.justnpcx.mirage.database.RuleSetEntity
import moe.matsuri.nb4a.SingBoxOptions.RuleSet
import moe.matsuri.nb4a.SingBoxOptions.Rule_DefaultOptions
import moe.matsuri.nb4a.generateRuleSet
import moe.matsuri.nb4a.makeSingBoxRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Remote rule sets: the definition emitted for a subscription, and how a route rule resolves a
 * reference to one.
 *
 * Getting the tag resolution wrong is silent — a token that should be a rule set reference quietly
 * becomes a domain suffix instead, and the rule stops matching what the user meant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RuleSetConfigTest {

    private fun entity(
        tag: String = "netflix",
        url: String = "https://example.com/netflix.srs",
        format: String = "",
        interval: String = "",
    ) = RuleSetEntity(tag = tag, url = url, format = format, updateInterval = interval)

    @Test
    fun remoteDefinitionCarriesUrlAndRidesTheTunnel() {
        val remote = mapOf("netflix" to entity(format = "binary", interval = "12h"))
        val defs = mutableListOf<RuleSet>()
        generateRuleSet(listOf("netflix"), defs, remote)

        assertEquals(1, defs.size)
        val map = defs[0].asMap()
        assertEquals("remote", map["type"])
        assertEquals("netflix", map["tag"])
        assertEquals("https://example.com/netflix.srs", map["url"])
        assertEquals("binary", map["format"])
        assertEquals("12h", map["update_interval"])
        // Always dialled through the proxy: leaving it off is deprecated, and our `bypass` outbound
        // is an empty direct one, which sing-box rejects as a detour target outright.
        @Suppress("UNCHECKED_CAST")
        val http = map["http_client"] as Map<*, *>
        assertEquals("proxy", http["detour"])
    }

    @Test
    fun remoteDefinitionOmitsEmptyOptionalFields() {
        val defs = mutableListOf<RuleSet>()
        generateRuleSet(listOf("netflix"), defs, mapOf("netflix" to entity()))

        val map = defs[0].asMap()
        assertFalse(map.containsKey("format"))
        assertFalse(map.containsKey("update_interval"))
        // `path` belongs to local rule sets only
        assertFalse(map.containsKey("path"))
    }

    @Test
    fun builtInGeoStillBecomesALocalRuleSet() {
        val defs = mutableListOf<RuleSet>()
        generateRuleSet(listOf("geosite:netflix", "geoip:cn"), defs)

        assertEquals(2, defs.size)
        for (def in defs) {
            val map = def.asMap()
            assertEquals("local", map["type"])
            assertEquals("binary", map["format"])
            assertFalse(map.containsKey("url"))
        }
    }

    @Test
    fun anUndeclaredTagIsNotEmitted() {
        val defs = mutableListOf<RuleSet>()
        generateRuleSet(listOf("nope"), defs, mapOf("netflix" to entity()))
        assertTrue(defs.isEmpty())
    }

    @Test
    fun aDeclaredTagIsResolvedFromTheDomainField() {
        val rule = Rule_DefaultOptions()
        rule.makeSingBoxRule(listOf("netflix", "example.com"), false, setOf("netflix"))

        assertEquals(listOf("netflix"), rule.rule_set)
        // the ordinary domain is untouched
        assertEquals(listOf("example.com"), rule.domain_suffix)
        assertNull(rule.domain)
    }

    @Test
    fun theRulesetPrefixIsResolvedToo() {
        val rule = Rule_DefaultOptions()
        rule.makeSingBoxRule(listOf("ruleset:netflix"), false, setOf("netflix"))
        assertEquals(listOf("netflix"), rule.rule_set)
        assertNull(rule.domain_suffix)
    }

    @Test
    fun anUndeclaredReferenceIsDroppedNotTreatedAsADomain() {
        // `ruleset:` is an explicit reference, so a typo must not silently become a domain suffix
        // that matches nothing.
        val rule = Rule_DefaultOptions()
        rule.makeSingBoxRule(listOf("ruleset:typo"), false, setOf("netflix"))
        assertNull(rule.rule_set)
        assertNull(rule.domain_suffix)
    }

    @Test
    fun anOrdinaryTokenIsStillADomainWhenNothingIsDeclared() {
        val rule = Rule_DefaultOptions()
        rule.makeSingBoxRule(listOf("example.com"), false, emptySet())
        assertEquals(listOf("example.com"), rule.domain_suffix)
        assertNull(rule.rule_set)
    }

    @Test
    fun aDeclaredTagIsResolvedFromTheIpField() {
        val rule = Rule_DefaultOptions()
        rule.makeSingBoxRule(listOf("netflix", "10.0.0.0/8"), true, setOf("netflix"))
        assertEquals(listOf("netflix"), rule.rule_set)
        assertEquals(listOf("10.0.0.0/8"), rule.ip_cidr)
    }
}
