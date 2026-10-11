package io.nekohasekai.sagernet.fmt.v2ray

import io.nekohasekai.sagernet.fmt.BuildSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end check that a parsed VLESS/REALITY link produces the expected sing-box outbound JSON.
 *
 * The builder used to read `DataStore` directly; the injected [BuildSettings] seam is what makes
 * this testable off-device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class V2RayOutboundTest {

    private class FakeSettings(override val globalAllowInsecure: Boolean) : BuildSettings

    private val link = "vless://15947d22-90ed-4111-bb72-bf63c947355e@api.justnpc.com:443" +
            "?type=tcp&encryption=none&security=reality" +
            "&pbk=50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA" +
            "&fp=chrome&sni=intel.com&sid=6ff8336d96&flow=xtls-rprx-vision" +
            "&support-x25519mlkem768=true#npc-speed"

    private fun vlessBean() = VMessBean().apply {
        initializeDefaultValues()
        alterId = -1
        parseDuckSoft(link)
    }

    private fun buildMap(settings: BuildSettings) =
        buildSingBoxOutboundStandardV2RayBean(vlessBean(), settings).asMap()

    @Test
    fun buildsVlessOutboundWithFlow() {
        val map = buildMap(FakeSettings(globalAllowInsecure = false))
        assertEquals("vless", map["type"])
        assertEquals("api.justnpc.com", map["server"])
        assertEquals(443, (map["server_port"] as Number).toInt())
        assertEquals("xtls-rprx-vision", map["flow"])
        assertEquals("15947d22-90ed-4111-bb72-bf63c947355e", map["uuid"])
    }

    @Test
    fun emitsRealityWithMlkemSupport() {
        val map = buildMap(FakeSettings(globalAllowInsecure = false))
        @Suppress("UNCHECKED_CAST")
        val tls = map["tls"] as Map<String, Any?>
        assertEquals(true, tls["enabled"])
        assertEquals("intel.com", tls["server_name"])

        @Suppress("UNCHECKED_CAST")
        val reality = tls["reality"] as Map<String, Any?>
        assertEquals(true, reality["enabled"])
        assertEquals("50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA", reality["public_key"])
        assertEquals("6ff8336d96", reality["short_id"])
        assertEquals(true, reality["support_x25519mlkem768"])

        @Suppress("UNCHECKED_CAST")
        val utls = tls["utls"] as Map<String, Any?>
        assertEquals(true, utls["enabled"])
        assertEquals("chrome", utls["fingerprint"])
    }

    @Test
    fun globalAllowInsecureIsInjected() {
        @Suppress("UNCHECKED_CAST")
        val insecureOn = (buildMap(FakeSettings(globalAllowInsecure = true))["tls"] as Map<String, Any?>)["insecure"]
        assertEquals(true, insecureOn)

        @Suppress("UNCHECKED_CAST")
        val insecureOff = (buildMap(FakeSettings(globalAllowInsecure = false))["tls"] as Map<String, Any?>)["insecure"]
        assertEquals(false, insecureOff)
    }
}
