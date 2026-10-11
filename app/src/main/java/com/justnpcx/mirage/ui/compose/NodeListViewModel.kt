package com.justnpcx.mirage.ui.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.justnpcx.mirage.database.DataStore
import com.justnpcx.mirage.database.SagerDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        DataStore.selectedProxy = id
        DataStore.currentProfile = id
        refresh()
    }
}
