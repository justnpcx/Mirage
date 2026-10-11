package com.justnpcx.mirage.ui.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.justnpcx.mirage.SagerNet
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.ProfileManager
import com.justnpcx.mirage.database.SagerDatabase
import kotlinx.coroutines.Dispatchers
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NodeItem(
    val id: Long,
    val name: String,
    val address: String,
    /** milliseconds, 0 when never tested */
    val ping: Int,
    val selected: Boolean,
)

data class NodeListState(
    val nodes: List<NodeItem> = emptyList(),
    val groupName: String = "",
)

class NodeListViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(NodeListState())

    private companion object {
        const val PING_TIMEOUT_MS = 3000
    }
    val state: StateFlow<NodeListState> = _state.asStateFlow()

    fun refresh() {
        val groupId = DataStore.currentGroupId()
        val selectedId = DataStore.selectedProxy
        val entities = runCatching { SagerDatabase.proxyDao.getByGroup(groupId) }.getOrDefault(emptyList())
        _state.value = NodeListState(
            nodes = entities.map { entity ->
                NodeItem(
                    id = entity.id,
                    name = entity.displayName(),
                    address = entity.displayAddress(),
                    ping = entity.ping,
                    selected = entity.id == selectedId,
                )
            },
            groupName = runCatching { SagerDatabase.groupDao.getById(groupId)?.displayName() }
                .getOrNull()
                .orEmpty(),
        )
    }

    fun select(id: Long) {
        val previous = DataStore.selectedProxy
        DataStore.selectedProxy = id
        DataStore.currentProfile = id
        // tell the running service to switch, otherwise the tap appears to do nothing
        if (DataStore.serviceState.canStop) {
            SagerNet.reloadService()
        }
        viewModelScope.launch { runCatching { ProfileManager.postUpdate(previous, true) } }
        refresh()
    }

    /** TCP-connect probe; stores the latency (or -1 on failure) and refreshes. */
    fun ping(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = runCatching { SagerDatabase.proxyDao.getById(id) }.getOrNull() ?: return@launch
            val bean = runCatching { entity.requireBean() }.getOrNull() ?: return@launch
            val started = System.currentTimeMillis()
            val reachable = runCatching {
                Socket().use {
                    it.connect(InetSocketAddress(bean.serverAddress, bean.serverPort), PING_TIMEOUT_MS)
                }
            }.isSuccess
            entity.ping = if (reachable) (System.currentTimeMillis() - started).toInt() else -1
            runCatching { SagerDatabase.proxyDao.updateProxy(entity) }
            refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { ProfileManager.deleteProfile(DataStore.currentGroupId(), id) }
            refresh()
        }
    }

    /** Live reorder while dragging; call [persistOrder] once the drag ends. */
    fun move(from: Int, to: Int) {
        val nodes = _state.value.nodes
        if (from !in nodes.indices || to !in nodes.indices || from == to) return
        val reordered = nodes.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        _state.value = _state.value.copy(nodes = reordered)
    }

    /** Writes the current order back to the database (userOrder = position + 1). */
    fun persistOrder() {
        val order = _state.value.nodes.map { it.id }
        if (order.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val entities = SagerDatabase.proxyDao.getEntities(order)
                val byId = entities.associateBy { it.id }
                val updated = order.mapIndexedNotNull { index, id ->
                    byId[id]?.also { it.userOrder = (index + 1).toLong() }
                }
                if (updated.isNotEmpty()) SagerDatabase.proxyDao.updateProxy(updated)
            }
        }
    }
}
