package com.justnpcx.mirage.fmt

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Picks a reachable DNS server from the user's configured list.
 *
 * sing-box has **no server-level DNS failover** (its `race`/`speculative` options are for
 * response-based rule matching), so if the first configured resolver is blocked by the ISP the
 * whole config fails to resolve the proxy server's address — issue #1176.
 *
 * The probe is a plain TCP connect, which is exactly the failure mode at play (the address being
 * blocked / blackholed). It is:
 *
 * - **fail-open**: if nothing answers, the original order is kept, so a false negative can only
 *   reorder the list, never empty it;
 * - **cached** for [CACHE_TTL_MS] so a reconnect does not pay for it again;
 * - skipped entirely for exports, so the generated config stays deterministic.
 */
object DnsHealth {

    private const val PROBE_TIMEOUT_MS = 1200
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    @Volatile
    private var cacheKey: List<String>? = null

    @Volatile
    private var cacheValue: List<String>? = null

    @Volatile
    private var cacheAt: Long = 0L

    /**
     * Returns [candidates] reordered so that the servers that answered a probe come first.
     * Order is otherwise preserved. Safe to call with 0 or 1 entries (returns them unchanged).
     */
    @Synchronized
    fun reorder(candidates: List<String>): List<String> {
        if (candidates.size <= 1) return candidates

        val now = System.currentTimeMillis()
        val cached = cacheValue
        if (cached != null && cacheKey == candidates && now - cacheAt < CACHE_TTL_MS) {
            return cached
        }

        val reachable = candidates.filter(::isReachable)
        val result = if (reachable.isEmpty()) {
            candidates
        } else {
            reachable + candidates.filterNot { it in reachable }
        }

        cacheKey = candidates
        cacheValue = result
        cacheAt = now
        return result
    }

    /** Clears the memoised ordering (e.g. after the user edits the DNS settings). */
    fun invalidate() {
        cacheKey = null
        cacheValue = null
        cacheAt = 0L
    }

    private fun isReachable(address: String): Boolean {
        val target = probeTarget(address) ?: return true // not a network endpoint (local/dhcp)
        return runCatching {
            Socket().use {
                it.connect(InetSocketAddress(target.first, target.second), PROBE_TIMEOUT_MS)
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Derives a `host to port` that proves reachability for a DNS address, mirroring the
     * transport mapping used when the config is generated.
     */
    fun probeTarget(address: String): Pair<String, Int>? {
        val value = address.trim()
        return when {
            value.isEmpty() -> null

            value == "local" || value.startsWith("dhcp://") || value.startsWith("rcode://") -> null

            value.startsWith("https://") -> hostPort(value.removePrefix("https://").substringBefore('/'), 443)
            value.startsWith("h3://") -> hostPort(value.removePrefix("h3://").substringBefore('/'), 443)
            value.startsWith("tls://") -> hostPort(value.removePrefix("tls://"), 853)
            value.startsWith("quic://") -> hostPort(value.removePrefix("quic://"), 853)
            value.startsWith("tcp://") -> hostPort(value.removePrefix("tcp://"), 53)
            value.startsWith("udp://") -> hostPort(value.removePrefix("udp://"), 53)

            else -> hostPort(value, 53)
        }
    }

    private fun hostPort(value: String, defaultPort: Int): Pair<String, Int> {
        val target = value.trim()
        // bracketed IPv6 must be split on ']' — a ':' split would cut inside the address
        if (target.startsWith("[")) {
            val close = target.indexOf(']')
            if (close > 0) {
                val host = target.substring(1, close)
                val port = target.substring(close + 1).removePrefix(":").toIntOrNull() ?: defaultPort
                return host to port
            }
        }
        val host = target.substringBefore(':')
        val port = target.substringAfter(':', "").toIntOrNull() ?: defaultPort
        return host to port
    }
}
