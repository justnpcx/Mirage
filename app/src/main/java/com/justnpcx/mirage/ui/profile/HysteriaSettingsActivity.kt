package com.justnpcx.mirage.ui.profile

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.justnpcx.mirage.Key
import com.justnpcx.mirage.R
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.preference.EditTextPreferenceModifiers
import com.justnpcx.mirage.fmt.hysteria.HysteriaBean
import com.justnpcx.mirage.ktx.applyDefaultValues
import moe.matsuri.nb4a.ui.SimpleMenuPreference

class HysteriaSettingsActivity : ProfileSettingsActivity<HysteriaBean>() {

    override fun createEntity() = HysteriaBean().applyDefaultValues()

    override fun HysteriaBean.init() {
        DataStore.profileName = name
        DataStore.protocolVersion = protocolVersion
        DataStore.serverAddress = serverAddress
        DataStore.serverPorts = serverPorts
        DataStore.serverObfs = obfuscation
        DataStore.serverAuthType = authPayloadType
        DataStore.serverProtocolInt = protocol
        DataStore.serverPassword = authPayload
        DataStore.serverSNI = sni
        DataStore.serverALPN = alpn
        DataStore.serverCertificates = caText
        DataStore.serverAllowInsecure = allowInsecure
        DataStore.serverUploadSpeed = uploadMbps
        DataStore.serverDownloadSpeed = downloadMbps
        DataStore.serverStreamReceiveWindow = streamReceiveWindow
        DataStore.serverConnectionReceiveWindow = connectionReceiveWindow
        DataStore.serverDisableMtuDiscovery = disableMtuDiscovery
        DataStore.serverHopInterval = hopInterval
        DataStore.serverObfsType = obfsType
        DataStore.serverHopIntervalMax = hopIntervalMax
        DataStore.serverGeckoMinPacketSize = geckoMinPacketSize
        DataStore.serverGeckoMaxPacketSize = geckoMaxPacketSize
        DataStore.serverBbrProfile = bbrProfile
        DataStore.serverDisableChromeParrot = disableChromeParrot
    }

    override fun HysteriaBean.serialize() {
        name = DataStore.profileName
        protocolVersion = DataStore.protocolVersion
        serverAddress = DataStore.serverAddress
        serverPorts = DataStore.serverPorts
        obfuscation = DataStore.serverObfs
        authPayloadType = DataStore.serverAuthType
        authPayload = DataStore.serverPassword
        protocol = DataStore.serverProtocolInt
        sni = DataStore.serverSNI
        alpn = DataStore.serverALPN
        caText = DataStore.serverCertificates
        allowInsecure = DataStore.serverAllowInsecure
        uploadMbps = DataStore.serverUploadSpeed
        downloadMbps = DataStore.serverDownloadSpeed
        streamReceiveWindow = DataStore.serverStreamReceiveWindow
        connectionReceiveWindow = DataStore.serverConnectionReceiveWindow
        disableMtuDiscovery = DataStore.serverDisableMtuDiscovery
        hopInterval = DataStore.serverHopInterval
        obfsType = DataStore.serverObfsType
        hopIntervalMax = DataStore.serverHopIntervalMax
        geckoMinPacketSize = DataStore.serverGeckoMinPacketSize
        geckoMaxPacketSize = DataStore.serverGeckoMaxPacketSize
        bbrProfile = DataStore.serverBbrProfile
        disableChromeParrot = DataStore.serverDisableChromeParrot
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.hysteria_preferences)

        val authType = findPreference<SimpleMenuPreference>(Key.SERVER_AUTH_TYPE)!!
        val authPayload = findPreference<EditTextPreference>(Key.SERVER_PASSWORD)!!
        authPayload.isVisible = authType.value != "${HysteriaBean.TYPE_NONE}"
        authType.setOnPreferenceChangeListener { _, newValue ->
            authPayload.isVisible = newValue != "${HysteriaBean.TYPE_NONE}"
            true
        }

        val protocol = findPreference<SimpleMenuPreference>(Key.SERVER_PROTOCOL)!!
        val alpn = findPreference<EditTextPreference>(Key.SERVER_ALPN)!!

        fun updateVersion(v: Int) {
            if (v == 2) {
                authPayload.isVisible = true
                //
                authType.isVisible = false
                protocol.isVisible = false
                alpn.isVisible = false
                //
                findPreference<EditTextPreference>(Key.SERVER_STREAM_RECEIVE_WINDOW)!!.isVisible =
                    false
                findPreference<EditTextPreference>(Key.SERVER_CONNECTION_RECEIVE_WINDOW)!!.isVisible =
                    false
                findPreference<SwitchPreference>(Key.SERVER_DISABLE_MTU_DISCOVERY)!!.isVisible =
                    false
                //
                authPayload.title = resources.getString(R.string.password)
            } else {
                authType.isVisible = true
                authPayload.isVisible = true
                protocol.isVisible = true
                alpn.isVisible = true
                //
                findPreference<EditTextPreference>(Key.SERVER_STREAM_RECEIVE_WINDOW)!!.isVisible =
                    true
                findPreference<EditTextPreference>(Key.SERVER_CONNECTION_RECEIVE_WINDOW)!!.isVisible =
                    true
                findPreference<SwitchPreference>(Key.SERVER_DISABLE_MTU_DISCOVERY)!!.isVisible =
                    true
                //
                authPayload.title = resources.getString(R.string.hysteria_auth_payload)
            }
        }
        findPreference<SimpleMenuPreference>(Key.PROTOCOL_VERSION)!!.setOnPreferenceChangeListener { _, newValue ->
            updateVersion(newValue.toString().toIntOrNull() ?: 1)
            true
        }
        updateVersion(DataStore.protocolVersion)

        findPreference<EditTextPreference>(Key.SERVER_UPLOAD_SPEED)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_DOWNLOAD_SPEED)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_STREAM_RECEIVE_WINDOW)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_CONNECTION_RECEIVE_WINDOW)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }

        findPreference<EditTextPreference>(Key.SERVER_PASSWORD)!!.apply {
            summaryProvider = PasswordSummaryProvider
        }
        findPreference<EditTextPreference>(Key.SERVER_OBFS)!!.apply {
            summaryProvider = PasswordSummaryProvider
        }

        findPreference<EditTextPreference>(Key.SERVER_HOP_INTERVAL)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_HOP_INTERVAL_MAX)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_GECKO_MIN_PACKET_SIZE)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }
        findPreference<EditTextPreference>(Key.SERVER_GECKO_MAX_PACKET_SIZE)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }

        setupObfsTypeVisibility()
    }

    /**
     * Only the gecko packet-size fields are type-specific: both obfs types need the password
     * (sing-box refuses to start with "missing obfs password").
     */
    private fun PreferenceFragmentCompat.setupObfsTypeVisibility() {
        val obfsType = findPreference<SimpleMenuPreference>(Key.SERVER_OBFS_TYPE)!!
        val geckoMin = findPreference<EditTextPreference>(Key.SERVER_GECKO_MIN_PACKET_SIZE)!!
        val geckoMax = findPreference<EditTextPreference>(Key.SERVER_GECKO_MAX_PACKET_SIZE)!!
        fun update(type: String?) {
            val gecko = type == "gecko"
            geckoMin.isVisible = gecko
            geckoMax.isVisible = gecko
        }
        update(obfsType.value)
        obfsType.setOnPreferenceChangeListener { _, newValue ->
            update(newValue as String?)
            true
        }
    }

}
