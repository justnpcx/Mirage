package com.justnpcx.mirage.core.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class V2RayUriTest {

    private val realityBase = "vless://15947d22-90ed-4111-bb72-bf63c947355e@api.justnpc.com:443" +
            "?type=tcp&encryption=none&security=reality" +
            "&pbk=50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA" +
            "&fp=chrome&sni=intel.com&sid=6ff8336d96&spx=%2F317d8b5fffaef89" +
            "&flow=xtls-rprx-vision"

    private val realityLink get() = "$realityBase#npc-speed"

    /** Appends a query parameter *before* the fragment. */
    private fun realityWith(param: String) = "$realityBase&$param#npc-speed"

    private fun parse(link: String, isTrojan: Boolean = false, isVless: Boolean = false) =
        V2RayUri.parse(ProxyUriParser.parse(link), isTrojan = isTrojan, isVless = isVless)

    @Test
    fun parsesRealityVlessWithMlkem() {
        val p = parse(realityWith("support-x25519mlkem768=true"), isVless = true)
        assertEquals("tcp", p.type)
        assertEquals("tls", p.security)
        assertEquals("intel.com", p.sni)
        assertEquals("chrome", p.fingerprint)
        assertEquals("xtls-rprx-vision", p.encryption)
        assertEquals("50So5LVE8EUUpvQttI0C9Qx1TqXrg2675Log_h228AA", p.realityPublicKey)
        assertEquals("6ff8336d96", p.realityShortId)
        assertTrue(p.realityMlkem)
    }

    @Test
    fun realityMlkemDisabledByDefault() {
        assertFalse(parse(realityLink, isVless = true).realityMlkem)
    }

    @Test
    fun realityMlkemAcceptsOne() {
        assertTrue(parse(realityWith("support-x25519mlkem768=1"), isVless = true).realityMlkem)
    }

    @Test
    fun realityMlkemRejectsFalse() {
        assertFalse(parse(realityWith("support-x25519mlkem768=false"), isVless = true).realityMlkem)
    }

    @Test
    fun realityFragmentIsPreserved() {
        assertEquals("npc-speed", ProxyUriParser.parse(realityLink).fragment)
    }

    @Test
    fun trojanDefaultsToTls() {
        val p = parse("trojan://pw@example.com:443?sni=example.com", isTrojan = true)
        assertEquals("tls", p.security)
        assertEquals("example.com", p.sni)
    }

    @Test
    fun plainLinkDefaultsToNoneSecurity() {
        assertEquals("none", parse("vless://u@h:443?type=tcp", isVless = true).security)
    }

    @Test
    fun sniFallsBackToHostParameter() {
        val p = parse("vless://u@h:443?security=tls&host=cdn.example.com", isVless = true)
        assertEquals("cdn.example.com", p.sni)
    }

    @Test
    fun wsTransportCarriesHostPathAndEarlyData() {
        val p = parse(
            "vless://u@h:443?type=ws&security=tls&host=a.example.com&path=%2Fws&ed=2048&eh=Sec-WebSocket-Protocol",
            isVless = true
        )
        assertEquals("ws", p.type)
        assertEquals("a.example.com", p.host)
        assertEquals("/ws", p.path)
        assertEquals(2048, p.earlyData)
        assertEquals("Sec-WebSocket-Protocol", p.earlyDataHeader)
    }

    @Test
    fun grpcTransportUsesServiceNameAsPath() {
        val p = parse("vless://u@h:443?type=grpc&serviceName=my-service", isVless = true)
        assertEquals("grpc", p.type)
        assertEquals("my-service", p.path)
    }

    @Test
    fun h2AndHttpHeaderTypeMapToHttp() {
        assertEquals("http", parse("vless://u@h:443?type=h2").type)
        assertEquals("http", parse("vless://u@h:443?headerType=http").type)
    }

    @Test
    fun httpTransportCarriesHostAndPath() {
        val p = parse("vless://u@h:443?type=http&host=a.example.com&path=%2Fx")
        assertEquals("http", p.type)
        assertEquals("a.example.com", p.host)
        assertEquals("/x", p.path)
    }

    @Test
    fun vmessCipherReadFromEncryptionParameter() {
        val p = parse("vmess://u@h:443?encryption=aes-128-gcm", isVless = false)
        assertEquals("aes-128-gcm", p.encryption)
    }

    @Test
    fun flowStripsUdp443Suffix() {
        assertEquals("xtls-rprx-vision", parse("vless://u@h:443?flow=xtls-rprx-vision-udp443", isVless = true).encryption)
    }

    @Test
    fun packetEncodingMapping() {
        assertEquals(1, parse("vless://u@h:443?packetEncoding=packet").packetEncoding)
        assertEquals(2, parse("vless://u@h:443?packetEncoding=xudp").packetEncoding)
        assertEquals(0, parse("vless://u@h:443").packetEncoding)
    }

    @Test
    fun allowInsecureAcceptsOneAndTrue() {
        assertTrue(parse("vless://u@h:443?security=tls&allowInsecure=1").allowInsecure)
        assertTrue(parse("vless://u@h:443?security=tls&allowInsecure=true").allowInsecure)
        assertFalse(parse("vless://u@h:443?security=tls&allowInsecure=0").allowInsecure)
    }

    @Test
    fun pathFromUriIsUsedWhenPresent() {
        val p = parse("vless://u@h:443/some/path?type=tcp")
        assertEquals("some/path", p.path)
    }

    @Test
    fun parsesXhttpTransportWithModeAndExtra() {
        val link = "vless://uuid@host:443?type=xhttp&security=tls&path=%2Fxhttp%2F" +
                "&host=cdn.example.com&mode=stream-up" +
                "&extra=%7B%22xPaddingBytes%22%3A%22100-1000%22%7D"
        val p = parse(link, isVless = true)
        assertEquals("xhttp", p.type)
        assertEquals("/xhttp/", p.path)
        assertEquals("cdn.example.com", p.host)
        assertEquals("stream-up", p.xhttpMode)
        assertEquals("{\"xPaddingBytes\":\"100-1000\"}", p.xhttpExtra)
    }

    @Test
    fun xhttpModeAndExtraDefaultToEmpty() {
        val p = parse("vless://uuid@host:443?type=xhttp", isVless = true)
        assertEquals("xhttp", p.type)
        assertEquals("", p.xhttpMode)
        assertEquals("", p.xhttpExtra)
    }
}
