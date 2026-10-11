package io.nekohasekai.sagernet.ui.compose

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ToolbarFragment
import io.nekohasekai.sagernet.ui.compose.theme.MirageTheme

/**
 * Home / Dashboard — the first Compose vertical slice.
 *
 * The screen is a pure function of [DashboardViewModel.state]; the fragment only hosts it and
 * forwards user intents. This is the template for migrating the remaining screens.
 */
class DashboardFragment : ToolbarFragment(R.layout.layout_compose) {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_home)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MirageTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = state,
                    onToggleConnection = viewModel::toggleConnection,
                    onOpenProfiles = {
                        (activity as? MainActivity)?.displayFragmentWithId(R.id.nav_configuration)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
