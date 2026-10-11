package com.justnpcx.mirage.core.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProxyUriParserTest {

    @Test
    fun parsesSchemeUserInfoHostPortAndFragment() {
        val uri = ProxyUriParser.parse(
            "vless://15947d22-90ed-4111-bb72-bf63c947355e@api.justnpc.com:443?type=tcp#npc-speed"
        )
        assertEquals("vless", uri.scheme)
        assertEquals("15947d22-90ed-4111-bb72-bf63c947355e", uri.username)
        assertEquals("", uri.password)
        assertEquals("api.justnpc.com", uri.host)
        assertEquals(443, uri.port)
        assertEquals("npc-speed", uri.fragment)
        assertEquals("tcp", uri.query("type"))
    }

    @Test
    fun lowercasesHost() {
        assertEquals("example.com", ProxyUriParser.parse("trojan://pw@ExAmPle.COM:443").host)
    }

    @Test
    fun defaultsPortWhenAbsent() {
        assertEquals(443, ProxyUriParser.parse("trojan://pw@example.com").port)
    }

    @Test
    fun parsesIpv6Host() {
        val uri = ProxyUriParser.parse("trojan://pw@[2001:db8::1]:8443")
        assertEquals("2001:db8::1", uri.host)
        assertEquals(8443, uri.port)
    }

    @Test
    fun parsesUserAndPassword() {
        val uri = ProxyUriParser.parse("trojan://user:pass@example.com:443")
        assertEquals("user", uri.username)
        assertEquals("pass", uri.password)
    }

    @Test
    fun percentDecodesQueryValues() {
        val uri = ProxyUriParser.parse(
            "vless://u@h:443?spx=%2F317d8b5fffaef89&flow=xtls-rprx-vision"
        )
        assertEquals("/317d8b5fffaef89", uri.query("spx"))
        assertEquals("xtls-rprx-vision", uri.query("flow"))
    }

    @Test
    fun plusIsSpaceInQueryButNotInFragment() {
        val uri = ProxyUriParser.parse("vless://u@h:443?ps=a+b#c+d")
        assertEquals("a b", uri.query("ps"))
        assertEquals("c+d", uri.fragment)
    }

    @Test
    fun parsesEmptyValuedQueryParameter() {
        val uri = ProxyUriParser.parse("vless://u@h:443?flag&x=1")
        assertEquals("", uri.query("flag"))
        assertEquals("1", uri.query("x"))
    }

    @Test
    fun keepsFirstDuplicateQueryParameter() {
        val uri = ProxyUriParser.parse("vless://u@h:443?a=1&a=2")
        assertEquals("1", uri.query("a"))
    }

    @Test
    fun splitsPathSegments() {
        assertEquals(listOf("a", "b"), ProxyUriParser.parse("vless://u@h:443/a/b").pathSegments)
        assertEquals(listOf(""), ProxyUriParser.parse("vless://u@h:443/").pathSegments)
        assertEquals(listOf(""), ProxyUriParser.parse("vless://u@h:443").pathSegments)
        assertEquals("a/b", ProxyUriParser.parse("vless://u@h:443/a/b").path)
    }

    @Test
    fun decodesUtf8PercentEscapes() {
        val uri = ProxyUriParser.parse("vless://u@h:443?name=%E4%B8%AD%E6%96%87")
        assertEquals("中文", uri.query("name"))
    }

    @Test
    fun rejectsLinkWithoutScheme() {
        assertFailsWith<IllegalArgumentException> { ProxyUriParser.parse("not-a-link") }
    }
}
