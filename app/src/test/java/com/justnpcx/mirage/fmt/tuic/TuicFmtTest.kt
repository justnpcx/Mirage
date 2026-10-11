package com.justnpcx.mirage.fmt.tuic

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TUIC share-link parsing.
 *
 * Issue #1090: links produced by v2rayN / 3x-ui encode the whole `uuid:password` pair into the
 * user-info (`tuic://uuid%3Apassword@host`), which used to end up entirely in the UUID field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TuicFmtTest {

    private val uuid = "15947d22-90ed-4111-bb72-bf63c947355e"
    private val password = "s3cret-pass"

    @Test
    fun parsesPercentEncodedUuidAndPassword() {
        // v2rayN / 3x-ui style: the colon is percent-encoded
        val bean = parseTuic("tuic://$uuid%3A$password@example.com:443#node")
        assertEquals(uuid, bean.uuid)
        assertEquals(password, bean.token)
        assertEquals("example.com", bean.serverAddress)
        assertEquals(443, bean.serverPort)
        assertEquals("node", bean.name)
    }

    @Test
    fun parsesLiteralUuidAndPassword() {
        // literal colon in the user-info
        val bean = parseTuic("tuic://$uuid:$password@example.com:443#node")
        assertEquals(uuid, bean.uuid)
        assertEquals(password, bean.token)
    }

    @Test
    fun parsesUuidOnlyWithPasswordParameter() {
        val bean = parseTuic("tuic://$uuid@example.com:443?congestion_control=bbr")
        assertEquals(uuid, bean.uuid)
        assertEquals("", bean.token)
        assertEquals("bbr", bean.congestionController)
    }

    @Test
    fun parsesQueryOptions() {
        val bean = parseTuic(
            "tuic://$uuid%3A$password@example.com:443" +
                "?sni=example.com&alpn=h3&udp_relay_mode=native&allow_insecure=1&disable_sni=1"
        )
        assertEquals("example.com", bean.sni)
        assertEquals("h3", bean.alpn)
        assertEquals("native", bean.udpRelayMode)
        assertEquals(true, bean.allowInsecure)
        assertEquals(true, bean.disableSNI)
    }
}
