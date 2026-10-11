package com.justnpcx.mirage.core.subscription

/**
 * Maps the "ducksoft" VLESS/VMess/Trojan share-link format onto transport/TLS options.
 *
 * See https://github.com/XTLS/Xray-core/issues/91.
 */
object V2RayUri {

    data class Parsed(
        /** V2Ray transport: tcp / http / ws / grpc / httpupgrade. */
        val type: String,
        /** `none` or `tls` (REALITY is represented as `tls` plus a public key). */
        val security: String,
        val sni: String,
        val alpn: String,
        val certificates: String,
        val allowInsecure: Boolean,
        /** WebSocket / HTTP / HTTPUpgrade Host header. */
        val host: String,
        val path: String,
        /** VLESS flow (e.g. `xtls-rprx-vision`) or VMess cipher for non-VLESS. */
        val encryption: String,
        val fingerprint: String,
        val realityPublicKey: String,
        val realityShortId: String,
        /**
         * Whether the server supports the post-quantum `X25519MLKEM768` key exchange.
         * Required by Xray-core >= v26.9.8 REALITY servers.
         */
        val realityMlkem: Boolean,
        val earlyData: Int,
        val earlyDataHeader: String,
        /** 0 = default, 1 = packet, 2 = xudp. */
        val packetEncoding: Int,
    )

    private data class Tls(
        val security: String,
        val sni: String = "",
        val alpn: String = "",
        val certificates: String = "",
        val allowInsecure: Boolean = false,
        val realityPublicKey: String = "",
        val realityShortId: String = "",
        val realityMlkem: Boolean = false,
    )

    private data class Transport(
        val host: String = "",
        val path: String = "",
        val earlyData: Int = 0,
        val earlyDataHeader: String = "",
    )

    fun parse(uri: ProxyUri, isTrojan: Boolean = false, isVless: Boolean = false): Parsed {
        val type = parseType(uri)
        val tls = parseTls(uri, isTrojan)
        val transport = parseTransport(uri, type)
        return Parsed(
            type = type,
            security = tls.security,
            sni = tls.sni,
            alpn = tls.alpn,
            certificates = tls.certificates,
            allowInsecure = tls.allowInsecure,
            host = transport.host,
            path = transport.path,
            encryption = parseEncryption(uri, isTrojan, isVless),
            fingerprint = uri.query("fp") ?: "",
            realityPublicKey = tls.realityPublicKey,
            realityShortId = tls.realityShortId,
            realityMlkem = tls.realityMlkem,
            earlyData = transport.earlyData,
            earlyDataHeader = transport.earlyDataHeader,
            packetEncoding = parsePacketEncoding(uri),
        )
    }

    private fun parseType(uri: ProxyUri): String {
        val type = uri.query("type") ?: "tcp"
        return if (type == "h2" || uri.query("headerType") == "http") "http" else type
    }

    private fun parseTls(uri: ProxyUri, isTrojan: Boolean): Tls {
        val security = uri.query("security").orEmpty().ifBlank { if (isTrojan) "tls" else "none" }
        if (security != "tls" && security != "reality") {
            return Tls(security = security)
        }
        val sni = uri.query("sni").orEmpty().ifBlank { uri.query("host").orEmpty() }
        return Tls(
            security = "tls",
            sni = sni,
            alpn = uri.query("alpn").orEmpty(),
            certificates = uri.query("cert").orEmpty(),
            allowInsecure = uri.query("allowInsecure").isTruthy(),
            realityPublicKey = uri.query("pbk").orEmpty(),
            realityShortId = uri.query("sid").orEmpty(),
            realityMlkem = uri.query("support-x25519mlkem768").isTruthy(),
        )
    }

    private fun parseTransport(uri: ProxyUri, type: String): Transport {
        // Path carried in the URI itself (rare); transport-specific params override it.
        val uriPath = if (uri.pathSegments.size > 1 || uri.pathSegments[0].isNotBlank()) uri.path else ""
        return when (type) {
            "http" -> Transport(host = uri.query("host").orEmpty(), path = uri.query("path") ?: uriPath)

            "ws" -> {
                val earlyData = uri.query("ed")
                Transport(
                    host = uri.query("host").orEmpty(),
                    path = uri.query("path") ?: uriPath,
                    earlyData = earlyData?.toIntOrNull() ?: 0,
                    earlyDataHeader = if (earlyData != null) uri.query("eh").orEmpty() else "",
                )
            }

            "grpc" -> Transport(path = uri.query("serviceName") ?: uriPath)

            "httpupgrade" -> Transport(host = uri.query("host").orEmpty(), path = uri.query("path") ?: uriPath)

            else -> Transport(path = uriPath)
        }
    }

    private fun parseEncryption(uri: ProxyUri, isTrojan: Boolean, isVless: Boolean): String = when {
        isVless -> uri.query("flow")?.removeSuffix("-udp443").orEmpty()
        isTrojan -> ""
        // VMess cipher (may come from a Matsuri export).
        else -> uri.query("encryption").orEmpty()
    }

    private fun parsePacketEncoding(uri: ProxyUri): Int = when (uri.query("packetEncoding")) {
        "packet" -> 1
        "xudp" -> 2
        else -> 0
    }

    private fun String?.isTruthy(): Boolean = this == "1" || this == "true"
}
