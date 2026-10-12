package com.justnpcx.mirage.fmt

import moe.matsuri.nb4a.SingBoxOptions.CacheFile
import moe.matsuri.nb4a.SingBoxOptions.ExperimentalOptions
import moe.matsuri.nb4a.SingBoxOptions.Outbound_SelectorOptions
import moe.matsuri.nb4a.SingBoxOptions.Outbound_URLTestOptions
import moe.matsuri.nb4a.SingBoxOptions.RouteOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locks down the group / route / cache-file parts of the generated config.
 *
 * These are the pieces that are easy to get subtly wrong and hard to notice: an outbound that
 * silently reroutes everything, a urltest that carries a selector-only field, a cache file that
 * writes to a path the app cannot open. Assert the serialized keys directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GroupAndRouteConfigTest {

    @Test
    fun urltestPicksItsOwnMemberAndKeepsDefaults() {
        val map = Outbound_URLTestOptions().apply {
            type = "urltest"
            tag = "proxy"
            outbounds = listOf("node-a", "node-b")
            url = "http://cp.cloudflare.com/"
        }.asMap()

        assertEquals("urltest", map["type"])
        assertEquals("proxy", map["tag"])
        assertEquals(listOf("node-a", "node-b"), map["outbounds"])
        assertEquals("http://cp.cloudflare.com/", map["url"])
        // `default` is a selector concept -- a urltest chooses its member by itself
        assertFalse(map.containsKey("default"))
        // interval / tolerance are left at the sing-box defaults (3m / 50ms)
        assertFalse(map.containsKey("interval"))
        assertFalse(map.containsKey("tolerance"))
    }

    @Test
    fun selectorKeepsItsDefaultMember() {
        val map = Outbound_SelectorOptions().apply {
            type = "selector"
            tag = "proxy"
            default_ = "node-b"
            outbounds = listOf("node-a", "node-b")
        }.asMap()

        assertEquals("selector", map["type"])
        assertEquals("node-b", map["default"])
    }

    @Test
    fun routeFinalIsEmittedUnderTheFinalKey() {
        val map = RouteOptions().apply { final_ = "proxy" }.asMap()
        // Without this, sing-box falls back to whichever outbound was created first, so the
        // catch-all silently depends on outbound ordering.
        assertEquals("proxy", map["final"])
    }

    @Test
    fun cacheFilePersistsFakeIpWithoutAPath() {
        val map = ExperimentalOptions().apply {
            cache_file = CacheFile().apply {
                enabled = true
                store_fakeip = true
            }
        }.asMap()

        val cacheFile = map["cache_file"] as Map<*, *>
        assertEquals(true, cacheFile["enabled"])
        assertEquals(true, cacheFile["store_fakeip"])
        // No explicit path: libbox chdirs into a writable directory at startup, so the default
        // `cache.db` lands somewhere the app owns. A hardcoded absolute path would break exports.
        assertFalse(cacheFile.containsKey("path"))
    }
}
