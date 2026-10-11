package com.justnpcx.mirage.core.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ShadowsocksUriTest {

    // base64("aes-256-gcm:pass")
    private val b64MethodPassword = "YWVzLTI1Ni1nY206cGFzcw=="

    // base64("aes-256-gcm:pass@1.2.3.4:8388")
    private val b64Whole = "YWVzLTI1Ni1nY206cGFzc0AxLjIuMy40OjgzODg="

    @Test
    fun parsesAndroidStyleWithBase64UserInfo() {
        val ss = ShadowsocksUri.parse("ss://$b64MethodPassword@1.2.3.4:8388#node")
        assertEquals("1.2.3.4", ss.host)
        assertEquals(8388, ss.port)
        assertEquals("aes-256-gcm", ss.method)
        assertEquals("pass", ss.password)
        assertEquals("", ss.plugin)
        assertEquals("node", ss.name)
    }

    @Test
    fun acceptsUrlSafeBase64WithoutPadding() {
        // "YWVzLTI1Ni1nY206cGFzcw" == base64url("aes-256-gcm:pass") without padding
        val ss = ShadowsocksUri.parse("ss://YWVzLTI1Ni1nY206cGFzcw@1.2.3.4:8388")
        assertEquals("aes-256-gcm", ss.method)
        assertEquals("pass", ss.password)
    }

    @Test
    fun parsesAndroidStyleWithPlainUserInfo() {
        val ss = ShadowsocksUri.parse("ss://aes-256-gcm:pass@1.2.3.4:8388")
        assertEquals("aes-256-gcm", ss.method)
        assertEquals("pass", ss.password)
        assertEquals(8388, ss.port)
    }

    @Test
    fun parsesPluginParameter() {
        val ss = ShadowsocksUri.parse(
            "ss://$b64MethodPassword@1.2.3.4:8388?plugin=obfs-local%3Bobfs%3Dhttp#n"
        )
        assertEquals("obfs-local;obfs=http", ss.plugin)
    }

    @Test
    fun parsesV2RayNStyle() {
        val ss = ShadowsocksUri.parse("ss://$b64Whole#my%20node")
        assertEquals("1.2.3.4", ss.host)
        assertEquals(8388, ss.port)
        assertEquals("aes-256-gcm", ss.method)
        assertEquals("pass", ss.password)
        assertEquals("my node", ss.name)
    }

    @Test
    fun v2RayNStyleWithoutFragmentHasEmptyName() {
        assertEquals("", ShadowsocksUri.parse("ss://$b64Whole").name)
    }

    @Test
    fun rejectsNonShadowsocksLink() {
        assertFailsWith<IllegalArgumentException> { ShadowsocksUri.parse("vless://u@h:443") }
    }

    @Test
    fun rejectsUndecodableBody() {
        assertFailsWith<IllegalArgumentException> { ShadowsocksUri.parse("ss://@@@###") }
    }

    @Test
    fun decodeBase64LooseAcceptsAllAlphabets() {
        assertEquals("aes-256-gcm:pass", ShadowsocksUri.decodeBase64Loose(b64MethodPassword))
        assertEquals("aes-256-gcm:pass", ShadowsocksUri.decodeBase64Loose("YWVzLTI1Ni1nY206cGFzcw"))
        assertEquals("aes-256-gcm:pass", ShadowsocksUri.decodeBase64Loose("YWVzLTI1Ni1nY206cGFzcw=="))
        assertNull(ShadowsocksUri.decodeBase64Loose(""))
    }
}
