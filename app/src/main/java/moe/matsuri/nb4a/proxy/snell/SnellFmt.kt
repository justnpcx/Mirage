package moe.matsuri.nb4a.proxy.snell

import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

/**
 * Snell (1.14).
 *
 * v4 and v6 share the outer shape but take different extra options, and sing-box merges whichever
 * set matches the version inline -- so obfs_* and mode are flat fields here too, and emitting the
 * wrong set is a config error rather than a harmless extra field.
 *
 * Deliberately avoids com.justnpcx.mirage.ktx helpers: that package's <clinit> reflects on a
 * device-only Socket API, which makes anything importing it untestable off-device.
 */
fun buildSingBoxOutboundSnellBean(bean: SnellBean): SingBoxOptions.Outbound_SnellOptions {
    return SingBoxOptions.Outbound_SnellOptions().apply {
        type = "snell"
        server = bean.serverAddress
        server_port = bean.serverPort
        version = bean.version
        psk = bean.psk
        bean.userKey?.takeIf { it.isNotBlank() }?.let { userkey = it }
        if (bean.reuse == true) reuse = true
        bean.network?.takeIf { it.isNotBlank() }?.let { network = it.listByLineOrComma() }

        if (bean.version == 4) {
            bean.obfsMode?.takeIf { it.isNotBlank() }?.let { obfs_mode = it }
            bean.obfsHost?.takeIf { it.isNotBlank() }?.let { obfs_host = it }
        } else {
            bean.mode?.takeIf { it.isNotBlank() }?.let { mode = it }
        }
    }
}
