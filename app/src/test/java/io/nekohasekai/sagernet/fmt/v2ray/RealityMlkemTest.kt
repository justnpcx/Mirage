package io.nekohasekai.sagernet.fmt.v2ray

import moe.matsuri.nb4a.SingBoxOptions.OutboundRealityOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundTLSOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the REALITY / X25519MLKEM768 support added for Xray-core >= v26.9.8 servers.
 *
 * Uses a plain [android.app.Application] because the real [io.nekohasekai.sagernet.SagerNet]
 * application cannot be constructed off-device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RealityMlkemTest {

    private val baseQuery = "vless://15947d22-90ed-4111-bb72-bf63c947355e@api.justnpc.com:443" +
            "?type=tcp&encryption=none&security=reality" +
            "&pbk=50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA" +
            "&fp=chrome&sni=intel.com&sid=6ff8336d96&spx=%2F317d8b5fffaef89" +
            "&flow=xtls-rprx-vision"

    private val fragment = "#npc-speed"

    private val baseLink get() = baseQuery + fragment

    /** Appends a query parameter *before* the fragment. */
    private fun linkWith(param: String) = "$baseQuery&$param$fragment"

    /**
     * Parses a vless:// link the same way [parseV2Ray] does for the "ducksoft" format.
     *
     * [parseV2Ray] itself is avoided because its error path calls `Logs`, and the `ktx` package
     * static-initializes an Android-only `Socket` reflection hack that does not exist on the JVM.
     *
     * The URI/field logic itself lives in `:core:subscription`; this is an integration check that
     * the parsed values are mapped onto the Android bean correctly.
     */
    private fun parse(link: String): StandardV2RayBean = VMessBean().apply {
        initializeDefaultValues()
        alterId = -1
        parseDuckSoft(link)
    }

    @Test
    fun parsesRealityFields() {
        val bean = parse(baseLink)
        assertEquals("api.justnpc.com", bean.serverAddress)
        assertEquals(443, bean.serverPort)
        assertEquals("tls", bean.security)
        assertEquals("xtls-rprx-vision", bean.encryption)
        assertEquals("50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA", bean.realityPubKey)
        assertEquals("6ff8336d96", bean.realityShortId)
        assertEquals("intel.com", bean.sni)
        assertEquals("chrome", bean.utlsFingerprint)
        assertEquals("npc-speed", bean.name)
    }

    @Test
    fun mlkemDisabledWhenFlagAbsent() {
        assertFalse(parse(baseLink).realityMlkem)
    }

    @Test
    fun mlkemEnabledByFlagTrue() {
        assertTrue(parse(linkWith("support-x25519mlkem768=true")).realityMlkem)
    }

    @Test
    fun mlkemEnabledByFlagOne() {
        assertTrue(parse(linkWith("support-x25519mlkem768=1")).realityMlkem)
    }

    @Test
    fun mlkemDisabledByFlagFalse() {
        assertFalse(parse(linkWith("support-x25519mlkem768=false")).realityMlkem)
    }

    /**
     * The JSON key must be exactly `support_x25519mlkem768` — it is what the sing-box fork reads.
     */
    @Test
    fun realityOptionsEmitSupportX25519Mlkem768Key() {
        val reality = OutboundRealityOptions().apply {
            enabled = true
            public_key = "50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA"
            short_id = "6ff8336d96"
            support_x25519mlkem768 = true
        }
        val map = reality.asMap()
        assertEquals(true, map["support_x25519mlkem768"])
        assertEquals("50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA", map["public_key"])
        assertEquals("6ff8336d96", map["short_id"])
    }

    @Test
    fun tlsOptionsNestRealityOptions() {
        val tls = OutboundTLSOptions().apply {
            enabled = true
            server_name = "intel.com"
            reality = OutboundRealityOptions().apply {
                enabled = true
                public_key = "pk"
                support_x25519mlkem768 = true
            }
        }
        @Suppress("UNCHECKED_CAST")
        val reality = tls.asMap()["reality"] as Map<String, Any?>
        assertEquals(true, reality["support_x25519mlkem768"])
    }
}
