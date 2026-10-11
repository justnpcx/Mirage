package com.justnpcx.mirage.bg.proto

import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.ProxyEntity

class UrlTest {

    val link = DataStore.connectionTestURL
    private val timeout = 5000

    suspend fun doTest(profile: ProxyEntity): Int {
        return TestInstance(profile, link, timeout).doTest()
    }

}