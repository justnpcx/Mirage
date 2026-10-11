package com.justnpcx.mirage.ui.compose

import com.justnpcx.mirage.fmt.AbstractBean
import com.justnpcx.mirage.fmt.http.HttpBean
import com.justnpcx.mirage.fmt.http.toUri
import com.justnpcx.mirage.fmt.hysteria.HysteriaBean
import com.justnpcx.mirage.fmt.hysteria.toUri
import com.justnpcx.mirage.fmt.shadowsocks.ShadowsocksBean
import com.justnpcx.mirage.fmt.shadowsocks.toUri
import com.justnpcx.mirage.fmt.socks.SOCKSBean
import com.justnpcx.mirage.fmt.socks.toUri
import com.justnpcx.mirage.fmt.trojan.TrojanBean
import com.justnpcx.mirage.fmt.tuic.TuicBean
import com.justnpcx.mirage.fmt.tuic.toUri
import com.justnpcx.mirage.fmt.v2ray.StandardV2RayBean
import com.justnpcx.mirage.fmt.v2ray.toUriVMessVLESSTrojan
import moe.matsuri.nb4a.proxy.anytls.AnyTLSBean
import moe.matsuri.nb4a.proxy.anytls.toUri

/**
 * Renders a node back into a share link, so the Compose node list can offer "share" without
 * depending on the legacy screens. Returns null for types without a link representation.
 */
fun AbstractBean.toShareLink(): String? = runCatching {
    when (this) {
        is StandardV2RayBean -> toUriVMessVLESSTrojan(this is TrojanBean)
        is ShadowsocksBean -> toUri()
        is HttpBean -> toUri()
        is SOCKSBean -> toUri()
        is TuicBean -> toUri()
        is HysteriaBean -> toUri()
        is AnyTLSBean -> toUri()
        else -> null
    }
}.getOrNull()
