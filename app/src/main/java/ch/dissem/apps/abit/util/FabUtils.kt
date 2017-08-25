package ch.dissem.apps.abit.util

import android.support.annotation.DrawableRes
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import io.github.kobakei.materialfabspeeddial.FabSpeedDial
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu

/**
 * Utilities to work with the common floating action button in the main activity
 */
object FabUtils {
    fun initFab(activity: MainActivity, @DrawableRes drawableRes: Int, menu: FabSpeedDialMenu): FabSpeedDial {
        val fab = activity.floatingActionButton
        fab.removeAllOnMenuItemClickListeners()
        fab.show()
        fab.closeMenu()
        val mainFab = fab.mainFab
        mainFab.setImageResource(drawableRes)
        fab.setMenu(menu)
        fab.addOnStateChangeListener { isOpened: Boolean ->
            if (isOpened) {
                // It will be turned 45 degrees, which makes an x out of the +
                mainFab.setImageResource(R.drawable.ic_action_add)
            } else {
                mainFab.setImageResource(drawableRes)
            }
        }
        return fab
    }
}
