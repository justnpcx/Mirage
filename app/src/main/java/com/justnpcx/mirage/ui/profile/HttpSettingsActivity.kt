package com.justnpcx.mirage.ui.profile

import com.justnpcx.mirage.fmt.http.HttpBean

class HttpSettingsActivity : StandardV2RaySettingsActivity() {

    override fun createEntity() = HttpBean()

}
