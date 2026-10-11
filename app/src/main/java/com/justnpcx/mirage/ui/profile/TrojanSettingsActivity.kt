package com.justnpcx.mirage.ui.profile

import com.justnpcx.mirage.fmt.trojan.TrojanBean

class TrojanSettingsActivity : StandardV2RaySettingsActivity() {

    override fun createEntity() = TrojanBean()

}
