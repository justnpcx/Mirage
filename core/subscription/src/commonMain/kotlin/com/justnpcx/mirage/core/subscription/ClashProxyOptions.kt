package com.justnpcx.mirage.core.subscription

/**
 * Transport / TLS / mux options extracted from a Clash(-Meta) proxy entry.
 *
 * The YAML tokenisation stays on the platform (the Android app uses snakeyaml), but the dense,
 * error-prone nested-map parsing now lives here where it can be unit tested on the JVM.
 *
 * Nullable fields mean "the key was absent, leave the target unchanged" — mirroring the original
 * `when (opt.key)` assignment semantics.
 */
data class ClashProxyOptions(
    /** `tls` when `tls: true` or a `reality-opts` block is present; null when absent. */
    val security: String? = null,
    val sni: String = "",
    /** Newline-joined `alpn`. */
    val alpn: String = "",
    val allowInsecure: Boolean? = null,
    val fingerprint: String = "",
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    /** `http` / `ws` / `grpc` / `httpupgrade`, or empty when `network` was absent. */
    val network: String = "",
    val host: String = "",
    val path: String = "",
    val maxEarlyData: Int? = null,
    val earlyDataHeaderName: String = "",
    val enableMux: Boolean? = null,
    val muxConcurrency: Int? = null,
    val muxPadding: Boolean? = null,
    val enableEch: Boolean? = null,
) {
    companion object {

        /** Clash `dummy` cipher maps to sing-box `none`. */
        fun normalizeCipher(cipher: String): String = if (cipher == "dummy") "none" else cipher

        /**
         * Builds the Shadowsocks plugin string (`name;opt=value;...`) from Clash's `plugin` and
         * `plugin-opts`.
         */
        fun shadowsocksPlugin(proxy: Map<String, Any?>): String {
            val plugin = proxy["plugin"]?.toString() ?: return ""
            val opts = proxy["plugin-opts"] as? Map<*, *> ?: return ""
            return when (plugin) {
                "obfs" -> listOf(
                    "obfs-local",
                    "obfs=" + (opts["mode"]?.toString() ?: ""),
                    "obfs-host=" + (opts["host"]?.toString() ?: ""),
                ).joinToString(";")

                "v2ray-plugin" -> buildList {
                    add("v2ray-plugin")
                    add("mode=" + (opts["mode"]?.toString() ?: ""))
                    if (opts["mode"]?.toString() == "true") add("tls")
                    add("host=" + (opts["host"]?.toString() ?: ""))
                    add("path=" + (opts["path"]?.toString() ?: ""))
                    if (opts["mux"]?.toString() == "true") add("mux=8")
                }.joinToString(";")

                else -> ""
            }
        }

        fun from(proxy: Map<String, Any?>): ClashProxyOptions {
            val builder = Builder()
            for ((key, value) in proxy) {
                if (builder.applyTls(key, value)) continue
                if (builder.applyTransport(key, value)) continue
                builder.applyMux(key, value)
            }
            return builder.build()
        }

        private class Builder {
            private var security: String? = null
            private var sni = ""
            private var alpn = ""
            private var allowInsecure: Boolean? = null
            private var fingerprint = ""
            private var realityPublicKey = ""
            private var realityShortId = ""
            private var network = ""
            private var host = ""
            private var path = ""
            private var maxEarlyData: Int? = null
            private var earlyDataHeaderName = ""
            private var enableMux: Boolean? = null
            private var muxConcurrency: Int? = null
            private var muxPadding: Boolean? = null
            private var enableEch: Boolean? = null

            fun applyTls(key: String, value: Any?): Boolean {
                when (key) {
                    "tls" -> security = if (value == true) "tls" else ""
                    "servername", "sni" -> sni = value?.toString().orEmpty()
                    "alpn" -> alpn = (value as? List<*>)?.joinToString("\n").orEmpty()
                    "skip-cert-verify" -> allowInsecure = value == true
                    "client-fingerprint" -> fingerprint = value?.toString().orEmpty()
                    "reality-opts" -> applyReality(value)
                    else -> return false
                }
                return true
            }

            private fun applyReality(value: Any?) {
                val reality = value as? Map<*, *> ?: return
                for ((realityKey, realityValue) in reality) {
                    security = "tls"
                    when (realityKey) {
                        "public-key" -> realityPublicKey = realityValue?.toString().orEmpty()
                        "short-id" -> realityShortId = realityValue?.toString().orEmpty()
                    }
                }
            }

            fun applyTransport(key: String, value: Any?): Boolean {
                when (key) {
                    "network" -> when (value) {
                        "h2", "http" -> network = "http"
                        "ws", "grpc", "xhttp" -> network = value as String
                    }

                    "ws-opts" -> applyWebsocket(value)
                    "h2-opts" -> applyH2(value)
                    "http-opts" -> applyHttp(value)
                    "grpc-opts" -> applyGrpc(value)

                    "xhttp-opts" -> applyXhttp(value)
                    else -> return false
                }
                return true
            }

            private fun applyWebsocket(value: Any?) {
                val ws = value as? Map<*, *> ?: return
                for ((wsKey, wsValue) in ws) {
                    when (wsKey) {
                        "headers" -> host = hostHeader(wsValue)
                        "path" -> path = wsValue?.toString().orEmpty()
                        "max-early-data" -> maxEarlyData = wsValue?.toString()?.toIntOrNull()
                        "early-data-header-name" -> earlyDataHeaderName = wsValue?.toString().orEmpty()
                        "v2ray-http-upgrade" -> if (wsValue == true) network = "httpupgrade"
                    }
                }
            }

            private fun applyH2(value: Any?) {
                val h2 = value as? Map<*, *> ?: return
                for ((h2Key, h2Value) in h2) {
                    when (h2Key) {
                        "host" -> host = (h2Value as? List<*>)?.joinToString("\n").orEmpty()
                        "path" -> path = h2Value?.toString().orEmpty()
                    }
                }
            }

            private fun applyHttp(value: Any?) {
                val http = value as? Map<*, *> ?: return
                for ((httpKey, httpValue) in http) {
                    when (httpKey) {
                        "path" -> path = (httpValue as? List<*>)?.joinToString("\n").orEmpty()
                        "headers" -> hostHeader(httpValue).let { if (it.isNotEmpty()) host = it }
                    }
                }
            }

            private fun applyXhttp(value: Any?) {
                val xhttp = value as? Map<*, *> ?: return
                for ((xhttpKey, xhttpValue) in xhttp) {
                    when (xhttpKey) {
                        "host" -> host = xhttpValue?.toString().orEmpty()
                        "path" -> path = xhttpValue?.toString().orEmpty()
                    }
                }
            }

            private fun applyGrpc(value: Any?) {
                val grpc = value as? Map<*, *> ?: return
                for ((grpcKey, grpcValue) in grpc) {
                    if (grpcKey == "grpc-service-name") path = grpcValue?.toString().orEmpty()
                }
            }

            /** Reads the `Host` header, which may be a scalar or a list. */
            private fun hostHeader(value: Any?): String {
                val headers = value as? Map<*, *> ?: return ""
                for ((headerKey, headerValue) in headers) {
                    if (headerKey.toString().lowercase() != "host") continue
                    return (headerValue as? List<*>)?.joinToString("\n")
                        ?: headerValue?.toString().orEmpty()
                }
                return ""
            }

            fun applyMux(key: String, value: Any?): Boolean {
                when (key) {
                    "smux" -> applySmux(value)
                    "ech-opts" -> applyEch(value)
                    else -> return false
                }
                return true
            }

            private fun applySmux(value: Any?) {
                val smux = value as? Map<*, *> ?: return
                for ((smuxKey, smuxValue) in smux) {
                    when (smuxKey) {
                        "enabled" -> enableMux = smuxValue.toString() == "true"
                        "max-streams" -> muxConcurrency = smuxValue.toString().toIntOrNull()
                        "padding" -> muxPadding = smuxValue.toString() == "true"
                    }
                }
            }

            private fun applyEch(value: Any?) {
                val ech = value as? Map<*, *> ?: return
                for ((echKey, echValue) in ech) {
                    if (echKey == "enable") enableEch = echValue.toString() == "true"
                }
            }

            fun build() = ClashProxyOptions(
                security = security,
                sni = sni,
                alpn = alpn,
                allowInsecure = allowInsecure,
                fingerprint = fingerprint,
                realityPublicKey = realityPublicKey,
                realityShortId = realityShortId,
                network = network,
                host = host,
                path = path,
                maxEarlyData = maxEarlyData,
                earlyDataHeaderName = earlyDataHeaderName,
                enableMux = enableMux,
                muxConcurrency = muxConcurrency,
                muxPadding = muxPadding,
                enableEch = enableEch,
            )
        }
    }
}
