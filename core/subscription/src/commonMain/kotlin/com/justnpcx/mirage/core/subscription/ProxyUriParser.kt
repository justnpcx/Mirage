package com.justnpcx.mirage.core.subscription

/**
 * A small, dependency-free parser for proxy share links
 * (`scheme://[user[:pass]@]host[:port][/path][?query][#fragment]`).
 *
 * It intentionally mirrors the parts of `okhttp3.HttpUrl` behaviour the app relied on:
 * percent-decoding of user-info, path, query and fragment, `+` treated as a space in query
 * components, and a scheme-dependent default port.
 */
object ProxyUriParser {

    private const val DEFAULT_PORT = 443

    fun parse(link: String): ProxyUri {
        val schemeEnd = link.indexOf("://")
        require(schemeEnd > 0) { "missing scheme in proxy link: $link" }
        val scheme = link.substring(0, schemeEnd).lowercase()
        var rest = link.substring(schemeEnd + 3)

        // fragment
        var fragment = ""
        val hash = rest.indexOf('#')
        if (hash >= 0) {
            fragment = decode(rest.substring(hash + 1), plusIsSpace = false)
            rest = rest.substring(0, hash)
        }

        // query
        var query: Map<String, String> = emptyMap()
        val questionMark = rest.indexOf('?')
        if (questionMark >= 0) {
            query = parseQuery(rest.substring(questionMark + 1))
            rest = rest.substring(0, questionMark)
        }

        // path
        var pathSegments = listOf("")
        val slash = rest.indexOf('/')
        if (slash >= 0) {
            pathSegments = splitPath(rest.substring(slash))
            rest = rest.substring(0, slash)
        }

        // authority: [user[:pass]@]host[:port]
        var username = ""
        var password = ""
        val at = rest.lastIndexOf('@')
        if (at >= 0) {
            val userInfo = rest.substring(0, at)
            val colon = userInfo.indexOf(':')
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon), plusIsSpace = true)
                password = decode(userInfo.substring(colon + 1), plusIsSpace = true)
            } else {
                username = decode(userInfo, plusIsSpace = true)
            }
            rest = rest.substring(at + 1)
        }

        val host: String
        var port = DEFAULT_PORT
        if (rest.startsWith("[")) {
            val close = rest.indexOf(']')
            require(close > 0) { "invalid IPv6 host in proxy link: $link" }
            host = rest.substring(1, close)
            val after = rest.substring(close + 1)
            if (after.startsWith(":")) {
                port = after.substring(1).toIntOrNull() ?: DEFAULT_PORT
            }
        } else {
            val colon = rest.lastIndexOf(':')
            if (colon >= 0) {
                host = rest.substring(0, colon)
                port = rest.substring(colon + 1).toIntOrNull() ?: DEFAULT_PORT
            } else {
                host = rest
            }
        }

        return ProxyUri(
            scheme = scheme,
            username = username,
            password = password,
            host = host.lowercase(),
            port = port,
            pathSegments = pathSegments,
            fragment = fragment,
            query = query,
        )
    }

    /**
     * Decodes a form/query component: percent escapes plus `+` as a space
     * (matches `java.net.URLDecoder`).
     */
    fun decodeFormComponent(value: String): String = decode(value, plusIsSpace = true)

    /** `"/a/b" -> ["a", "b"]`, `"/" -> [""]` (matches okhttp path segmentation). */
    private fun splitPath(rawPath: String): List<String> =
        rawPath.split('/').drop(1).map { decode(it, plusIsSpace = false) }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val name = if (eq >= 0) pair.substring(0, eq) else pair
            val value = if (eq >= 0) pair.substring(eq + 1) else ""
            val decodedName = decode(name, plusIsSpace = true)
            // okhttp's queryParameter() returns the first occurrence.
            if (!result.containsKey(decodedName)) {
                result[decodedName] = decode(value, plusIsSpace = true)
            }
        }
        return result
    }

    private fun decode(value: String, plusIsSpace: Boolean): String {
        if (!value.contains('%') && !(plusIsSpace && value.contains('+'))) return value
        val out = ArrayList<Byte>(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                c == '+' && plusIsSpace -> {
                    out.add(' '.code.toByte())
                    i++
                }

                c == '%' && i + 2 < value.length -> {
                    val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) {
                        out.add(hex.toByte())
                        i += 3
                    } else {
                        out.add(c.code.toByte())
                        i++
                    }
                }

                else -> {
                    for (b in c.toString().encodeToByteArray()) out.add(b)
                    i++
                }
            }
        }
        return out.toByteArray().decodeToString()
    }
}
