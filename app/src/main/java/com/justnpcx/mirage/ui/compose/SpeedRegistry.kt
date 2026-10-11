package com.justnpcx.mirage.ui.compose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live transfer rates, in bytes per second. */
data class SpeedStats(
    val txRate: Long = 0L,
    val rxRate: Long = 0L,
)

/**
 * Bridge between the service's speed callback (delivered to [MainActivity]) and the Compose
 * screens, which are pure functions of observable state.
 */
object SpeedRegistry {

    private val _speed = MutableStateFlow(SpeedStats())
    val speed: StateFlow<SpeedStats> = _speed.asStateFlow()

    fun update(txRate: Long, rxRate: Long) {
        _speed.value = SpeedStats(txRate, rxRate)
    }

    fun clear() {
        _speed.value = SpeedStats()
    }
}
