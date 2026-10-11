package com.justnpcx.mirage.fmt.wireguard

import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma

fun genReserved(anyStr: String): String {
    try {
        val list = anyStr.listByLineOrComma()
        val ba = ByteArray(3)
        if (list.size == 3) {
            list.forEachIndexed { index, s ->
                val i = s
                    .replace("[", "")
                    .replace("]", "")
                    .replace(" ", "")
                    .toIntOrNull() ?: return anyStr
                ba[index] = i.toByte()
            }
            return Util.b64EncodeOneLine(ba)
        } else {
            return anyStr
        }
    } catch (e: Exception) {
        return anyStr
    }
}

/**
 * Build a sing-box WireGuard *endpoint*.
 *
 * sing-box 1.13 removed the WireGuard outbound. Endpoints are still addressable by tag
 * ([adapter.Endpoint] embeds [adapter.Outbound] and the outbound manager resolves endpoint tags),
 * so selectors and route rules keep working unchanged after this migration.
 */
fun buildSingBoxEndpointWireguardBean(bean: WireGuardBean): SingBoxOptions.WireGuardEndpointOptions {
    return SingBoxOptions.WireGuardEndpointOptions().apply {
        type = "wireguard"
        mtu = bean.mtu
        address = bean.localAddress.listByLineOrComma()
        private_key = bean.privateKey
        peers = listOf(SingBoxOptions.WireGuardEndpointPeer().apply {
            address = bean.serverAddress
            port = bean.serverPort
            public_key = bean.peerPublicKey
            pre_shared_key = bean.peerPreSharedKey
            // Required by sing-box: a single-peer client routes all traffic through the peer.
            allowed_ips = listOf("0.0.0.0/0", "::/0")
            if (bean.reserved.isNotBlank()) reserved = genReserved(bean.reserved)
        })
    }
}
