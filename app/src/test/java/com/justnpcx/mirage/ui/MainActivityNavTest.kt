package com.justnpcx.mirage.ui

import android.view.Menu
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.justnpcx.mirage.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards the navigation drawer against "dead" items.
 *
 * Every item in `main_drawer_menu.xml` must be mapped to a screen by [fragmentForNavId]. A missing
 * branch is completely silent: `displayFragmentWithId` returns false, `onNavigationItemSelected`
 * returns false, the drawer does not even move its selection, and the user just sees nothing
 * happen. A drawer item once shipped dead exactly this way -- the menu item existed and the
 * fragment it should have opened existed too, but no branch ever opened it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MainActivityNavTest {

    private fun drawerMenu(): Menu {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SagerNet)
        val popup = PopupMenu(context, View(context))
        popup.menuInflater.inflate(R.menu.main_drawer_menu, popup.menu)
        return popup.menu
    }

    @Test
    fun everyDrawerItemOpensAScreen() {
        val menu = drawerMenu()
        assertTrue("main_drawer_menu.xml did not parse", menu.size() > 0)

        val dead = (0 until menu.size())
            .map { menu.getItem(it) }
            .filter { fragmentForNavId(it.itemId) == null }
            .map { it.title.toString() }

        assertEquals("drawer items with no screen wired up", emptyList<String>(), dead)
    }
}
