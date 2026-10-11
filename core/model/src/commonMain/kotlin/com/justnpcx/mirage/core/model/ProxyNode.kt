package com.justnpcx.mirage.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Platform-agnostic description of a proxy node.
 *
 * This is the seed of the shared (Kotlin Multiplatform) core: the same model will eventually be
 * produced by the subscription parsers on Android and consumed by other targets (desktop/iOS)
 * without depending on Android APIs.
 */
@Serializable
data class ProxyNode(
    val id: Long = 0,
    val name: String = "",
    @SerialName("server") val serverAddress: String = "",
    @SerialName("server_port") val serverPort: Int = 0,
    val type: String = "",
) {
    /** "host:port" — mirrors the legacy `AbstractBean.displayAddress()` formatting. */
    fun displayAddress(): String = when {
        serverAddress.isEmpty() -> ""
        serverAddress.contains(':') -> "[$serverAddress]:$serverPort"
        else -> "$serverAddress:$serverPort"
    }
}

/** REALITY key-exchange capabilities advertised by a subscription link. */
@Serializable
enum class RealityKeyExchange {
    X25519,

    /** Post-quantum hybrid, required by Xray-core >= v26.9.8 REALITY servers. */
    X25519MLKEM768,
}
