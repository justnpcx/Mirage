package com.justnpcx.mirage.core.subscription

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A parsed `ss://` share link.
 */
data class ShadowsocksLink(
    val host: String,
    val port: Int,
    val method: String,
    val password: String,
    val plugin: String = "",
    val name: String = "",
)

/**
 * Parser for the two mainstream Shadowsocks link styles:
 *
 * - **ss-android**: `ss://[base64(method:password) | method:password]@host:port?plugin=...#name`
 * - **v2rayN**: `ss://base64(method:password@host:port)#name`
 */
object ShadowsocksUri {

    private const val SCHEME = "ss://"

    @OptIn(ExperimentalEncodingApi::class)
    private val base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    fun parse(link: String): ShadowsocksLink {
        require(link.startsWith(SCHEME, ignoreCase = true)) { "not a shadowsocks link: $link" }
        val withoutScheme = link.substring(SCHEME.length)
        val beforeFragment = withoutScheme.substringBefore('#')
        val rawFragment = withoutScheme.substringAfter('#', "")

        return if (beforeFragment.contains('@')) {
            parseAndroidStyle(link, beforeFragment)
        } else {
            parseV2RayNStyle(link, beforeFragment, rawFragment)
        }
    }

    private fun parseAndroidStyle(link: String, beforeFragment: String): ShadowsocksLink {
        var uri = ProxyUriParser.parse(link)

        if (uri.username.isBlank()) {
            // justmysocks ships links whose whole authority is base64 of "userinfo@host:port".
            val decoded = decodeBase64Loose(beforeFragment)
                ?: throw IllegalArgumentException("invalid jms link: $link")
            uri = ProxyUriParser.parse("$SCHEME$decoded")
        }

        val plugin = uri.query("plugin").orEmpty()
        return if (uri.password.isNotBlank()) {
            ShadowsocksLink(
                host = uri.host,
                port = uri.port,
                method = uri.username,
                password = uri.password,
                plugin = plugin,
                name = uri.fragment,
            )
        } else {
            val methodAndPassword = decodeBase64Loose(uri.username)
                ?: throw IllegalArgumentException("invalid ss link: $link")
            ShadowsocksLink(
                host = uri.host,
                port = uri.port,
                method = methodAndPassword.substringBefore(':'),
                password = methodAndPassword.substringAfter(':'),
                plugin = plugin,
                name = uri.fragment,
            )
        }
    }

    private fun parseV2RayNStyle(link: String, beforeFragment: String, rawFragment: String): ShadowsocksLink {
        val decoded = decodeBase64Loose(beforeFragment)
            ?: throw IllegalArgumentException("invalid v2rayN link: $link")
        val uri = ProxyUriParser.parse("$SCHEME$decoded")
        val remarks = ProxyUriParser.decodeFormComponent(rawFragment)
        return ShadowsocksLink(
            host = uri.host,
            port = uri.port,
            method = uri.username,
            password = uri.password,
            plugin = "",
            name = if (remarks.isNotBlank()) remarks else "",
        )
    }

    /**
     * Lenient base64 decoding: accepts the URL-safe alphabet, ignores whitespace and tolerates a
     * missing padding — mirroring the Android implementation this replaced.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64Loose(input: String): String? {
        val normalized = input
            .filterNot { it.isWhitespace() }
            .replace('-', '+')
            .replace('_', '/')
        if (normalized.isEmpty()) return null
        return runCatching { base64.decode(normalized) }
            .getOrNull()
            ?.decodeToString(throwOnInvalidSequence = false)
    }
}
