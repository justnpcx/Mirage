package com.justnpcx.mirage.ui.compose

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justnpcx.mirage.R
import com.justnpcx.mirage.database.SagerDatabase
import com.justnpcx.mirage.ktx.Logs
import com.justnpcx.mirage.ktx.onMainDispatcher
import com.justnpcx.mirage.ktx.runOnDefaultDispatcher
import com.justnpcx.mirage.ui.ToolbarFragment
import com.justnpcx.mirage.ui.compose.theme.MirageTheme

/**
 * Hosts the Compose node list. The existing Configuration screen stays available for the
 * operations it owns (drag reorder, subscriptions, per-node editing).
 */
class NodeListFragment : ToolbarFragment(R.layout.layout_compose) {

    private val viewModel: NodeListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_nodes)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MirageTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                NodeListScreen(
                    state = state,
                    onSelect = viewModel::select,
                    onMove = viewModel::move,
                    onMoveFinished = viewModel::persistOrder,
                    onEdit = ::editNode,
                    onShare = ::shareNode,
                    onPing = viewModel::ping,
                    onDelete = viewModel::delete,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun editNode(id: Long) {
        val context = context ?: return
        runOnDefaultDispatcher {
            val entity = SagerDatabase.proxyDao.getById(id) ?: return@runOnDefaultDispatcher
            onMainDispatcher {
                startActivity(entity.settingIntent(context, false))
            }
        }
    }

    private fun shareNode(id: Long) {
        Logs.w(Throwable("share sheet requested by NodeListFragment.shareNode(id=$id)"))
        val context = context ?: return
        runOnDefaultDispatcher {
            val entity = SagerDatabase.proxyDao.getById(id) ?: return@runOnDefaultDispatcher
            val link = entity.requireBean().toShareLink() ?: entity.displayAddress()
            onMainDispatcher {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link)
                }
                startActivity(Intent.createChooser(send, getString(R.string.node_action_share)))
            }
        }
    }
}
