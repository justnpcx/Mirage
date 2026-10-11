package com.justnpcx.mirage.ui.compose

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justnpcx.mirage.R
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
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
