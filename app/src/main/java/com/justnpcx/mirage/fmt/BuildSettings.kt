package com.justnpcx.mirage.fmt

import com.justnpcx.mirage.database.DataStore

/**
 * The global preferences the sing-box config builders need.
 *
 * Reading [DataStore] directly inside the builders made them impossible to unit test off-device.
 * Injecting this seam keeps the builders pure; production code uses [DataStoreBuildSettings] via the
 * default argument, while tests pass a fake.
 *
 * This is the first step of the wider "inject the global singletons" refactor (the next step is a
 * ViewModel/DI layer on top).
 */
interface BuildSettings {
    /** Mirrors `DataStore.globalAllowInsecure`. */
    val globalAllowInsecure: Boolean
}

object DataStoreBuildSettings : BuildSettings {
    override val globalAllowInsecure: Boolean get() = DataStore.globalAllowInsecure
}
