package com.justnpcx.mirage.core.subscription

/**
 * A platform-agnostic view of a proxy share link.
 *
 * All textual components are already percent-decoded, so parsers can work on plain values.
 * This mirrors the semantics of `okhttp3.HttpUrl` (which the Android app used before this logic
 * was moved into the shared core).
 */
data class ProxyUri(
    val scheme: String,
    val username: String = "",
    val password: String = "",
    val host: String = "",
    val port: Int = 443,
    val pathSegments: List<String> = listOf(""),
    val fragment: String = "",
    val query: Map<String, String> = emptyMap(),
) {
    /** Joined path without a leading slash, e.g. `["a", "b"] -> "a/b"`. */
    val path: String get() = pathSegments.joinToString("/")

    fun query(name: String): String? = query[name]
}
