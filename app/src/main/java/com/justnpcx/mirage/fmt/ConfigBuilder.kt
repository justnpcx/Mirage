package com.justnpcx.mirage.fmt

import android.widget.Toast
import com.justnpcx.mirage.*
import com.justnpcx.mirage.bg.VpnService
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.ProxyEntity
import com.justnpcx.mirage.database.ProxyEntity.Companion.TYPE_CONFIG
import com.justnpcx.mirage.database.SagerDatabase
import com.justnpcx.mirage.fmt.ConfigBuildResult.IndexEntity
import com.justnpcx.mirage.fmt.hysteria.HysteriaBean
import com.justnpcx.mirage.fmt.hysteria.buildSingBoxOutboundHysteriaBean
import com.justnpcx.mirage.fmt.internal.ChainBean
import com.justnpcx.mirage.fmt.shadowsocks.ShadowsocksBean
import com.justnpcx.mirage.fmt.shadowsocks.buildSingBoxOutboundShadowsocksBean
import com.justnpcx.mirage.fmt.socks.SOCKSBean
import com.justnpcx.mirage.fmt.socks.buildSingBoxOutboundSocksBean
import com.justnpcx.mirage.fmt.ssh.SSHBean
import com.justnpcx.mirage.fmt.ssh.buildSingBoxOutboundSSHBean
import com.justnpcx.mirage.fmt.tuic.TuicBean
import com.justnpcx.mirage.fmt.tuic.buildSingBoxOutboundTuicBean
import com.justnpcx.mirage.fmt.v2ray.StandardV2RayBean
import com.justnpcx.mirage.fmt.v2ray.buildSingBoxOutboundStandardV2RayBean
import com.justnpcx.mirage.fmt.wireguard.WireGuardBean
import com.justnpcx.mirage.fmt.wireguard.buildSingBoxEndpointWireguardBean
import com.justnpcx.mirage.ktx.isIpAddress
import com.justnpcx.mirage.ktx.mkPort
import com.justnpcx.mirage.utils.PackageCache
import moe.matsuri.nb4a.*
import moe.matsuri.nb4a.SingBoxOptions.*
import moe.matsuri.nb4a.plugin.Plugins
import moe.matsuri.nb4a.proxy.anytls.AnyTLSBean
import moe.matsuri.nb4a.proxy.anytls.buildSingBoxOutboundAnyTLSBean
import moe.matsuri.nb4a.proxy.config.ConfigBean
import moe.matsuri.nb4a.proxy.shadowtls.ShadowTLSBean
import moe.matsuri.nb4a.proxy.shadowtls.buildSingBoxOutboundShadowTLSBean
import moe.matsuri.nb4a.utils.JavaUtil.gson
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

const val TAG_MIXED = "mixed-in"

const val TAG_PROXY = "proxy"
const val TAG_DIRECT = "direct"
const val TAG_BYPASS = "bypass"
const val TAG_BLOCK = "block"

const val LOCALHOST = "127.0.0.1"

class ConfigBuildResult(
    var config: String,
    var externalIndex: List<IndexEntity>,
    var mainEntId: Long,
    var trafficMap: Map<String, List<ProxyEntity>>,
    var profileTagMap: Map<Long, String>,
    val selectorGroupId: Long,
) {
    data class IndexEntity(var chain: LinkedHashMap<Int, ProxyEntity>)
}

fun buildConfig(
    proxy: ProxyEntity, forTest: Boolean = false, forExport: Boolean = false
): ConfigBuildResult {

    if (proxy.type == TYPE_CONFIG) {
        val bean = proxy.requireBean() as ConfigBean
        if (bean.type == 0) {
            return ConfigBuildResult(
                bean.config,
                listOf(),
                proxy.id, //
                mapOf(TAG_PROXY to listOf(proxy)), //
                mapOf(proxy.id to TAG_PROXY), //
                -1L
            )
        }
    }

    val trafficMap = HashMap<String, List<ProxyEntity>>()
    val tagMap = HashMap<Long, String>()
    val globalOutbounds = HashMap<Long, String>()
    val selectorNames = ArrayList<String>()
    val group = SagerDatabase.groupDao.getById(proxy.groupId)

    fun ProxyEntity.resolveChainInternal(): MutableList<ProxyEntity> {
        val bean = requireBean()
        if (bean is ChainBean) {
            val beans = SagerDatabase.proxyDao.getEntities(bean.proxies)
            val beansMap = beans.associateBy { it.id }
            val beanList = ArrayList<ProxyEntity>()
            for (proxyId in bean.proxies) {
                val item = beansMap[proxyId] ?: continue
                beanList.addAll(item.resolveChainInternal())
            }
            return beanList.asReversed()
        }
        return mutableListOf(this)
    }

    fun selectorName(name_: String): String {
        var name = name_
        var count = 0
        while (selectorNames.contains(name)) {
            count++
            name = "$name_-$count"
        }
        selectorNames.add(name)
        return name
    }

    fun ProxyEntity.resolveChain(): MutableList<ProxyEntity> {
        val thisGroup = SagerDatabase.groupDao.getById(groupId)
        val frontProxy = thisGroup?.frontProxy?.let { SagerDatabase.proxyDao.getById(it) }
        val landingProxy = thisGroup?.landingProxy?.let { SagerDatabase.proxyDao.getById(it) }
        val list = resolveChainInternal()
        if (frontProxy != null) {
            list.add(frontProxy)
        }
        if (landingProxy != null) {
            list.add(0, landingProxy)
        }
        return list
    }

    val extraRules = if (forTest) listOf() else SagerDatabase.rulesDao.enabledRules()
    val extraProxies =
        if (forTest) mapOf() else SagerDatabase.proxyDao.getEntities(extraRules.mapNotNull { rule ->
            rule.outbound.takeIf { it > 0 && it != proxy.id }
        }.toHashSet().toList()).associateBy { it.id }
    // Subscribed rule sets. sing-box fetches these itself; the app only emits the url and the tag.
    // Their tags become referenceable from route rules (see SingBoxOptionsUtil.resolveRuleSetTag).
    val enabledRuleSets = if (forTest || forExport) listOf() else SagerDatabase.ruleSetDao.enabledRuleSets()
    val remoteRuleSetMap = enabledRuleSets.associateBy { it.tag }
    val remoteRuleSetTags = remoteRuleSetMap.keys
    // urltest wins when both are somehow set: it is the stricter, more useful mode.
    val buildUrlTest = !forTest && group?.isUrlTest == true && !forExport
    val buildSelector = !forTest && !buildUrlTest && group?.isSelector == true && !forExport
    val userDNSRuleList = mutableListOf<DNSRule_DefaultOptions>()
    val domainListDNSDirectForce = mutableListOf<String>()
    val bypassDNSBeans = hashSetOf<AbstractBean>()
    val isVPN = DataStore.serviceMode == Key.MODE_VPN
    val bind = if (!forTest && DataStore.allowAccess) "0.0.0.0" else LOCALHOST
    fun parseDnsSetting(raw: String) = raw.split("\n")
        .mapNotNull { dns -> dns.trim().takeIf { it.isNotBlank() && !it.startsWith("#") } }

    // sing-box has no DNS failover, so pick a reachable server up front (issue #1176).
    // Exports stay deterministic, and the probe is fail-open.
    val remoteDns = parseDnsSetting(DataStore.remoteDns)
        .let { if (forExport) it else DnsHealth.reorder(it) }
    val directDNS = parseDnsSetting(DataStore.directDns)
        .let { if (forExport) it else DnsHealth.reorder(it) }
    val enableDnsRouting = DataStore.enableDnsRouting
    val useFakeDns = DataStore.enableFakeDns && !forTest
    val needSniff = DataStore.trafficSniffing > 0
    val needSniffOverride = DataStore.trafficSniffing == 2
    val externalIndexMap = ArrayList<IndexEntity>()
    val ipv6Mode = if (forTest) IPv6Mode.ENABLE else DataStore.ipv6Mode

    fun genDomainStrategy(noAsIs: Boolean): String {
        return when {
            !noAsIs -> ""
            ipv6Mode == IPv6Mode.DISABLE -> "ipv4_only"
            ipv6Mode == IPv6Mode.PREFER -> "prefer_ipv6"
            ipv6Mode == IPv6Mode.ONLY -> "ipv6_only"
            else -> "prefer_ipv4"
        }
    }

    return MyOptions().apply {
        if (!forTest && (DataStore.enableClashAPI || useFakeDns)) experimental = ExperimentalOptions().apply {
            if (DataStore.enableClashAPI) clash_api = ClashAPIOptions().apply {
                external_controller = "127.0.0.1:9090"
                external_ui = "../files/yacd"
            }
            // Persist the fakeip mappings (and nothing else) so they survive a restart. The path
            // is left unset: libbox chdirs into a writable directory at startup, so the default
            // `cache.db` lands somewhere the app owns.
            if (useFakeDns) cache_file = CacheFile().apply {
                enabled = true
                store_fakeip = true
            }
        }

        log = LogOptions().apply {
            level = when (DataStore.logLevel) {
                0 -> "panic"
                1 -> "warn"
                2 -> "info"
                3 -> "debug"
                4 -> "trace"
                else -> "info"
            }
        }

        dns = DNSOptions().apply {
            servers = mutableListOf()
            rules = mutableListOf()
            independent_cache = true
        }

        fun autoDnsDomainStrategy(s: String): String? {
            if (s.isNotEmpty()) {
                return s
            }
            return when (ipv6Mode) {
                IPv6Mode.DISABLE -> "ipv4_only"
                IPv6Mode.ENABLE -> "prefer_ipv4"
                IPv6Mode.PREFER -> "prefer_ipv6"
                IPv6Mode.ONLY -> "ipv6_only"
                else -> null
            }
        }

        inbounds = mutableListOf()

        if (!forTest) {
            if (isVPN) inbounds.add(Inbound_TunOptions().apply {
                type = "tun"
                tag = "tun-in"
                stack = when (DataStore.tunImplementation) {
                    TunImplementation.GVISOR -> "gvisor"
                    TunImplementation.SYSTEM -> "system"
                    else -> "mixed"
                }
                endpoint_independent_nat = true
                mtu = DataStore.mtu
                // sing-box 1.12 removed the legacy inet4_address / inet6_address fields.
                address = when (ipv6Mode) {
                    IPv6Mode.DISABLE -> listOf(VpnService.PRIVATE_VLAN4_CLIENT + "/28")

                    IPv6Mode.ONLY -> listOf(VpnService.PRIVATE_VLAN6_CLIENT + "/126")

                    else -> listOf(
                        VpnService.PRIVATE_VLAN4_CLIENT + "/28",
                        VpnService.PRIVATE_VLAN6_CLIENT + "/126",
                    )
                }
            })
            inbounds.add(Inbound_MixedOptions().apply {
                type = "mixed"
                tag = TAG_MIXED
                listen = bind
                listen_port = DataStore.mixedPort
            })
        }

        outbounds = mutableListOf()
        endpoints = mutableListOf()

        // init routing object
        route = RouteOptions().apply {
            auto_detect_interface = true
            rules = mutableListOf()
            rule_set = mutableListOf()
            // Pin the catch-all outbound explicitly. With `final` unset, sing-box falls back to the
            // *first outbound that was created* (adapter/outbound/manager.go), which happens to be
            // the proxy today only because we always insert it at index 0. Relying on that ordering
            // is fragile -- adding or reordering an outbound would silently reroute every unmatched
            // connection. Say what we mean: everything not matched by a rule goes through the proxy.
            final_ = TAG_PROXY
            // Every outbound turns its own server domain into an address through this resolver.
            // Left unset they fall back to the default DNS transport, which for us is dns-remote —
            // and dns-remote is dialled through the proxy. That is a cycle: the proxy cannot come
            // up until its own server name resolves. Pin it to the direct resolver, which is
            // reachable without the tunnel.
            default_domain_resolver = DomainResolveOptions().apply { server = "dns-direct" }
        }

        // sing-box 1.11+ replaced the legacy inbound sniff / resolve fields with rule actions.
        if (!forTest) {
            val sniffInbounds = if (isVPN) listOf("tun-in", TAG_MIXED) else listOf(TAG_MIXED)
            genDomainStrategy(DataStore.resolveDestination).takeIf { it.isNotEmpty() }?.let { strategy ->
                route.rules.add(Rule_DefaultOptions().apply {
                    inbound = sniffInbounds
                    action = "resolve"
                    this.strategy = strategy
                })
            }
            if (needSniff) {
                route.rules.add(Rule_DefaultOptions().apply {
                    inbound = sniffInbounds
                    action = "sniff"
                })
            }
        }

        // returns outbound tag
        fun buildChain(
            chainId: Long, entity: ProxyEntity
        ): String {
            val profileList = entity.resolveChain()
            val chainTrafficSet = HashSet<ProxyEntity>().apply {
                plusAssign(profileList)
                add(entity)
            }

            var currentOutbound: SingBoxOption
            lateinit var pastOutbound: SingBoxOption
            lateinit var pastInboundTag: String
            var pastEntity: ProxyEntity? = null
            val externalChainMap = LinkedHashMap<Int, ProxyEntity>()
            externalIndexMap.add(IndexEntity(externalChainMap))
            val chainOutbounds = ArrayList<SingBoxOption>()

            // chainTagOut: v2ray outbound tag for this chain
            var chainTagOut = ""
            val chainTag = "c-$chainId"
            var muxApplied = false

            val defaultServerDomainStrategy = SingBoxOptionsUtil.domainStrategy("server")

            profileList.forEachIndexed { index, proxyEntity ->
                val bean = proxyEntity.requireBean()

                // tagOut: v2ray outbound tag for a profile
                // profile2 (in) (global)   tag g-(id)
                // profile1                 tag (chainTag)-(id)
                // profile0 (out)           tag (chainTag)-(id) / single: "proxy"
                var tagOut = "$chainTag-${proxyEntity.id}"

                // needGlobal: can only contain one?
                var needGlobal = false

                // first profile set as global
                if (index == profileList.lastIndex) {
                    needGlobal = true
                    tagOut = "g-" + proxyEntity.id
                    bypassDNSBeans += proxyEntity.requireBean()
                }

                // last profile set as "proxy"
                if (chainId == 0L && index == 0) {
                    tagOut = TAG_PROXY
                }

                // selector human readable name
                if (buildSelector && index == 0) {
                    tagOut = selectorName(bean.displayName())
                }


                // chain rules
                if (index > 0) {
                    // chain route/proxy rules
                    if (pastEntity!!.needExternal()) {
                        route.rules.add(Rule_DefaultOptions().apply {
                            inbound = listOf(pastInboundTag)
                            outbound = tagOut
                        })
                    } else {
                        pastOutbound._hack_config_map["detour"] = tagOut
                    }
                } else {
                    // index == 0 means last profile in chain / not chain
                    chainTagOut = tagOut
                }

                // now tagOut is determined
                if (needGlobal) {
                    globalOutbounds[proxyEntity.id]?.let {
                        if (index == 0) chainTagOut = it // single, duplicate chain
                        return@forEachIndexed
                    }
                    globalOutbounds[proxyEntity.id] = tagOut
                }

                if (proxyEntity.needExternal()) { // externel outbound
                    val localPort = mkPort()
                    externalChainMap[localPort] = proxyEntity
                    currentOutbound = Outbound_SocksOptions().apply {
                        type = "socks"
                        server = LOCALHOST
                        server_port = localPort
                    }
                } else {
                    // internal outbound

                    currentOutbound = when (bean) {
                        is ConfigBean -> CustomSingBoxOption(bean.config)

                        is ShadowTLSBean -> // before StandardV2RayBean
                            buildSingBoxOutboundShadowTLSBean(bean)

                        is StandardV2RayBean -> // http/trojan/vmess/vless
                            buildSingBoxOutboundStandardV2RayBean(bean)

                        is HysteriaBean ->
                            buildSingBoxOutboundHysteriaBean(bean)

                        is TuicBean ->
                            buildSingBoxOutboundTuicBean(bean)

                        is SOCKSBean ->
                            buildSingBoxOutboundSocksBean(bean)

                        is ShadowsocksBean ->
                            buildSingBoxOutboundShadowsocksBean(bean)

                        is WireGuardBean ->
                            buildSingBoxEndpointWireguardBean(bean)

                        is SSHBean ->
                            buildSingBoxOutboundSSHBean(bean)

                        is AnyTLSBean ->
                            buildSingBoxOutboundAnyTLSBean(bean)

                        else -> throw IllegalStateException("can't reach")
                    }

                    // internal mux (not supported on endpoints)
                    if (!muxApplied && currentOutbound !is WireGuardEndpointOptions) {
                        val muxObj = proxyEntity.singMux()
                        if (muxObj != null && muxObj.enabled) {
                            muxApplied = true
                            currentOutbound._hack_config_map["multiplex"] = muxObj.asMap()
                        }
                    }
                }

                // internal & external
                currentOutbound.apply {
                    // udp over tcp
                    try {
                        val sUoT = bean.javaClass.getField("sUoT").get(bean)
                        if (sUoT is Boolean && sUoT) {
                            _hack_config_map["udp_over_tcp"] = true
                        }
                    } catch (_: Exception) {
                    }

                    // domain_strategy
                    pastEntity?.requireBean()?.apply {
                        // don't loopback
                        if (defaultServerDomainStrategy != "" && !serverAddress.isIpAddress()) {
                            domainListDNSDirectForce.add("full:$serverAddress")
                        }
                    }
                    _hack_config_map["domain_strategy"] =
                        if (forTest) "" else defaultServerDomainStrategy

                    _hack_config_map["tag"] = tagOut

                    _hack_custom_config = bean.customOutboundJson
                }

                // External proxy need a dokodemo-door inbound to forward the traffic
                // For external proxy software, their traffic must goes to v2ray-core to use protected fd.
                bean.finalAddress = bean.serverAddress
                bean.finalPort = bean.serverPort
                if (bean.canMapping() && proxyEntity.needExternal()) {
                    // With ss protect, don't use mapping
                    var needExternal = true
                    if (index == profileList.lastIndex) {
                        val pluginId = when (bean) {
                            is HysteriaBean -> if (bean.protocolVersion == 1) "hysteria-plugin" else "hysteria2-plugin"
                            else -> ""
                        }
                        if (Plugins.isUsingMatsuriExe(pluginId)) {
                            needExternal = false
                        } else if (Plugins.getPluginExternal(pluginId) != null) {
                            throw Exception("You are using an unsupported $pluginId, please download the correct plugin.")
                        }
                    }
                    if (needExternal) {
                        val mappingPort = mkPort()
                        bean.finalAddress = LOCALHOST
                        bean.finalPort = mappingPort

                        inbounds.add(Inbound_DirectOptions().apply {
                            type = "direct"
                            listen = LOCALHOST
                            listen_port = mappingPort
                            tag = "$chainTag-mapping-${proxyEntity.id}"

                            override_address = bean.serverAddress
                            override_port = bean.serverPort

                            pastInboundTag = tag

                            // no chain rule and not outbound, so need to set to direct
                            if (index == profileList.lastIndex) {
                                route.rules.add(Rule_DefaultOptions().apply {
                                    inbound = listOf(tag)
                                    outbound = TAG_DIRECT
                                })
                            }
                        })
                    }
                }

                // WireGuard is an endpoint in sing-box 1.13+; everything else stays an outbound.
                if (currentOutbound is WireGuardEndpointOptions) {
                    endpoints.add(currentOutbound)
                } else {
                    outbounds.add(currentOutbound)
                }
                chainOutbounds.add(currentOutbound)
                pastOutbound = currentOutbound
                pastEntity = proxyEntity
            }

            trafficMap[chainTagOut] = chainTrafficSet.toList()
            return chainTagOut
        }

        // build outbounds
        if (buildSelector || buildUrlTest) {
            val list = group.id.let { SagerDatabase.proxyDao.getByGroup(it) }
            list.forEach {
                tagMap[it.id] = buildChain(it.id, it)
            }
            val groupOutbound: SingBoxOption = if (buildUrlTest) {
                // `urltest` probes every member on its own and always dials the fastest one, so the
                // node picks itself. No `default` here -- that is a selector concept.
                Outbound_URLTestOptions().apply {
                    type = "urltest"
                    tag = TAG_PROXY
                    outbounds = tagMap.values.toList()
                    url = DataStore.connectionTestURL
                }
            } else {
                Outbound_SelectorOptions().apply {
                    type = "selector"
                    tag = TAG_PROXY
                    default_ = tagMap[proxy.id]
                    outbounds = tagMap.values.toList()
                }
            }
            outbounds.add(0, groupOutbound)
        } else {
            buildChain(0, proxy)
        }
        // build outbounds from route item
        extraProxies.forEach { (key, p) ->
            tagMap[key] = buildChain(key, p)
        }

        // apply user rules
        for (rule in extraRules) {
            if (rule.packages.isNotEmpty()) {
                PackageCache.awaitLoadSync()
            }
            val uidList = rule.packages.map {
                if (!isVPN) {
                    Toast.makeText(
                        SagerNet.application,
                        SagerNet.application.getString(R.string.route_need_vpn, rule.displayName()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                PackageCache[it]?.takeIf { uid -> uid >= 1000 }
            }.toHashSet().filterNotNull()
            val ruleSets = mutableListOf<RuleSet>()

            val ruleObj = Rule_DefaultOptions().apply {
                if (uidList.isNotEmpty()) {
                    PackageCache.awaitLoadSync()
                    user_id = uidList
                }
                var domainList: List<String>? = null
                if (rule.domains.isNotBlank()) {
                    domainList = rule.domains.listByLineOrComma()
                    makeSingBoxRule(domainList, false, remoteRuleSetTags)
                }
                if (rule.ip.isNotBlank()) {
                    makeSingBoxRule(rule.ip.listByLineOrComma(), true, remoteRuleSetTags)
                }

                if (rule_set != null) generateRuleSet(rule_set, ruleSets)

                if (rule.port.isNotBlank()) {
                    port = mutableListOf<Int>()
                    port_range = mutableListOf<String>()
                    rule.port.listByLineOrComma().map {
                        if (it.contains(":")) {
                            port_range.add(it)
                        } else {
                            it.toIntOrNull()?.apply { port.add(this) }
                        }
                    }
                }
                if (rule.sourcePort.isNotBlank()) {
                    source_port = mutableListOf<Int>()
                    source_port_range = mutableListOf<String>()
                    rule.sourcePort.listByLineOrComma().map {
                        if (it.contains(":")) {
                            source_port_range.add(it)
                        } else {
                            it.toIntOrNull()?.apply { source_port.add(this) }
                        }
                    }
                }
                if (rule.network.isNotBlank()) {
                    network = listOf(rule.network)
                }
                if (rule.source.isNotBlank()) {
                    source_ip_cidr = rule.source.listByLineOrComma()
                }
                if (rule.protocol.isNotBlank()) {
                    protocol = rule.protocol.listByLineOrComma()
                }

                fun makeDnsRuleObj(): DNSRule_DefaultOptions {
                    return DNSRule_DefaultOptions().apply {
                        if (uidList.isNotEmpty()) user_id = uidList
                        domainList?.let { makeSingBoxRule(it, remoteRuleSetTags) }
                    }
                }

                when (rule.outbound) {
                    -1L -> {
                        userDNSRuleList += makeDnsRuleObj().apply {
                            server = "dns-direct"
                            strategy = autoDnsDomainStrategy(SingBoxOptionsUtil.domainStrategy("dns-direct"))
                        }
                    }

                    0L -> {
                        if (useFakeDns) userDNSRuleList += makeDnsRuleObj().apply {
                            server = "dns-fake"
                            inbound = listOf("tun-in")
                        }
                        userDNSRuleList += makeDnsRuleObj().apply {
                            server = "dns-remote"
                        }
                    }

                    -2L -> {
                        userDNSRuleList += makeDnsRuleObj().apply {
                            // `rcode://success` is a rule action in sing-box 1.12+ (the rcode
                            // DNS server was removed in 1.14.0).
                            action = "predefined"
                            rcode = "NOERROR"
                        }
                    }
                }

                outbound = when (val outId = rule.outbound) {
                    0L -> TAG_PROXY
                    -1L -> TAG_BYPASS
                    -2L -> TAG_BLOCK
                    else -> if (outId == proxy.id) TAG_PROXY else tagMap[outId] ?: ""
                }

                _hack_custom_config = rule.config
            }

            if (!ruleObj.checkEmpty()) {
                if (ruleObj.outbound.isNullOrBlank()) {
                    Toast.makeText(
                        SagerNet.application,
                        "Warning: " + rule.displayName() + ": A non-existent outbound was specified.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // block 改用新的写法
                    if (ruleObj.outbound == TAG_BLOCK) {
                        ruleObj.outbound = null
                        ruleObj.action = "reject"
                    }
                    route.rules.add(ruleObj)
                    route.rule_set.addAll(ruleSets)
                }
            }
        }

        // Definitions for every enabled subscription, so one that no rule references yet is still
        // fetched and ready to use. Deduped below along with the per-rule local ones.
        if (enabledRuleSets.isNotEmpty()) {
            val remoteDefs = mutableListOf<RuleSet>()
            generateRuleSet(remoteRuleSetTags.toList(), remoteDefs, remoteRuleSetMap)
            route.rule_set.addAll(remoteDefs)
        }

        // 对 rule_set tag 去重
        if (route.rule_set != null) {
            route.rule_set = route.rule_set.distinctBy { it.tag }
        }

        for (freedom in arrayOf(TAG_DIRECT, TAG_BYPASS)) outbounds.add(Outbound().apply {
            tag = freedom
            type = "direct"
        })

        // Bypass Lookup for the first profile
        bypassDNSBeans.forEach {
            var serverAddr = it.serverAddress

            if (it is ConfigBean) {
                var config = mutableMapOf<String, Any>()
                config = gson.fromJson(it.config, config.javaClass)
                config["server"]?.apply {
                    serverAddr = toString()
                }
            }

            if (!serverAddr.isIpAddress()) {
                domainListDNSDirectForce.add("full:${serverAddr}")
            }
        }

        remoteDns.forEach {
            var address = it
            if (address.contains("://")) {
                address = address.substringAfter("://")
            }
            "https://$address".toHttpUrlOrNull()?.apply {
                if (!host.isIpAddress()) {
                    domainListDNSDirectForce.add("full:$host")
                }
            }
        }

        // No `detour` here on purpose: a DNS server without a detour dials directly, while
        // detouring to the (empty) `direct` outbound is rejected by sing-box 1.14 with
        // "detour to an empty direct outbound makes no sense".
        dns.servers.add(DNSServerOptions().apply {
            type = "local"
            tag = "dns-local"
        })

        directDNS.firstOrNull().let {
            dns.servers.add(DNSServerOptions().apply {
                applyDnsAddress(it ?: throw Exception("No direct DNS, check your settings!"))
                tag = "dns-direct"
                domain_resolver = "dns-local"
            })
        }

        remoteDns.firstOrNull().let {
            // Always use direct DNS for urlTest
            if (!forTest) dns.servers.add(DNSServerOptions().apply {
                applyDnsAddress(it ?: throw Exception("No remote DNS, check your settings!"))
                tag = "dns-remote"
                // A DNS server with no detour is dialled by a *plain* dialer: it does not consult
                // the route rules or route.final (see dns/transport_dialer.go -> dialer.NewWithOptions,
                // which only detours when `detour` is set). Left empty, a foreign DoH such as
                // dns.google is dialled straight out and dies on the way — which then takes every
                // non-CN lookup with it. Route it through the proxy.
                detour = TAG_PROXY
                domain_resolver = "dns-direct"
            })
        }

        dns.final_ = if (forTest) "dns-direct" else "dns-remote"

        // sing-box 1.12+ moved the per-server `strategy` to the dns object (global) and to the
        // rules referencing a specific server.
        dns.strategy = autoDnsDomainStrategy(SingBoxOptionsUtil.domainStrategy("dns-remote"))

        // dns object user rules
        if (enableDnsRouting) {
            userDNSRuleList.forEach {
                if (!it.checkEmpty()) dns.rules.add(it)
            }
        }

        if (forTest) {
            dns.rules = listOf()
        } else {
            // built-in DNS rules
            route.rules.add(0, Rule_DefaultOptions().apply {
                protocol = listOf("dns")
                action = "hijack-dns"
            })
            route.rules.add(0, Rule_DefaultOptions().apply {
                port = listOf(53)
                action = "hijack-dns"
            })
            if (DataStore.bypassLanInCore) {
                route.rules.add(Rule_DefaultOptions().apply {
                    outbound = TAG_BYPASS
                    ip_is_private = true
                })
            }
            // block mcast
            route.rules.add(Rule_DefaultOptions().apply {
                ip_cidr = listOf("224.0.0.0/3", "ff00::/8")
                source_ip_cidr = listOf("224.0.0.0/3", "ff00::/8")
                action = "reject"
            })
            // FakeDNS obj
            if (useFakeDns) {
                // sing-box 1.12+: fake IP is a typed DNS server; the `dns.fakeip` object was
                // removed in 1.14.0.
                dns.servers.add(DNSServerOptions().apply {
                    type = "fakeip"
                    tag = "dns-fake"
                    inet4_range = "198.18.0.0/15"
                    inet6_range = "fc00::/18"
                })
                dns.rules.add(DNSRule_DefaultOptions().apply {
                    inbound = listOf("tun-in")
                    server = "dns-fake"
                    disable_cache = true
                })
            }
            // avoid loopback
            dns.rules.add(0, DNSRule_DefaultOptions().apply {
                outbound = mutableListOf("any")
                server = "dns-direct"
                strategy = autoDnsDomainStrategy(SingBoxOptionsUtil.domainStrategy("dns-direct"))
            })
            // force bypass (always top DNS rule)
            if (domainListDNSDirectForce.isNotEmpty()) {
                dns.rules.add(0, DNSRule_DefaultOptions().apply {
                    makeSingBoxRule(domainListDNSDirectForce.toHashSet().toList())
                    server = "dns-direct"
                    strategy = autoDnsDomainStrategy(SingBoxOptionsUtil.domainStrategy("dns-direct"))
                })
            }
        }

        if (!forTest) _hack_custom_config = DataStore.globalCustomConfig
    }.let {
        val configMap = it.asMap()
        Util.mergeJSON(configMap, proxy.requireBean().customConfigJson)
        ConfigBuildResult(
            gson.toJson(configMap),
            externalIndexMap,
            proxy.id,
            trafficMap,
            tagMap,
            if (buildSelector) group.id else -1L
        )
    }

}

/**
 * Translates a legacy DNS "address" into the typed server form required by sing-box 1.12+
 * (the legacy form was removed in 1.14.0).
 */
private val DNS_SCHEME_TYPES = mapOf(
    "tcp://" to "tcp",
    "tls://" to "tls",
    "quic://" to "quic",
    "dhcp://" to "dhcp",
    "udp://" to "udp",
)

private fun DNSServerOptions.applyDnsAddress(address: String) {
    if (address == "local") {
        type = "local"
        return
    }

    val scheme = DNS_SCHEME_TYPES.entries.firstOrNull { address.startsWith(it.key) }
    if (scheme != null) {
        type = scheme.value
        server = address.removePrefix(scheme.key)
        return
    }

    if (address.startsWith("https://") || address.startsWith("h3://")) {
        type = if (address.startsWith("h3://")) "h3" else "https"
        val rest = address.substringAfter("://")
        server = rest.substringBefore("/")
        if (rest.contains("/")) path = "/" + rest.substringAfter("/")
        return
    }

    type = "udp"
    server = address
}
