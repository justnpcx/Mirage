package com.justnpcx.mirage.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyNodeTest {

    @Test
    fun displayAddress_formatsIpv4() {
        val node = ProxyNode(serverAddress = "1.2.3.4", serverPort = 443)
        assertEquals("1.2.3.4:443", node.displayAddress())
    }

    @Test
    fun displayAddress_wrapsIpv6() {
        val node = ProxyNode(serverAddress = "2001:db8::1", serverPort = 443)
        assertEquals("[2001:db8::1]:443", node.displayAddress())
    }

    @Test
    fun displayAddress_emptyServer() {
        assertEquals("", ProxyNode().displayAddress())
    }
}
