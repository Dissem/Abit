package ch.dissem.apps.abit.drawer

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentManager
import android.view.View
import android.widget.Toast

import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile

import ch.dissem.apps.abit.AddressDetailActivity
import ch.dissem.apps.abit.AddressDetailFragment
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.dialog.AddIdentityDialogFragment
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.entity.BitmessageAddress

import android.widget.Toast.LENGTH_LONG

class ProfileSelectionListener(
        private val ctx: Context,
        private val fragmentManager: FragmentManager
) : AccountHeader.OnAccountHeaderListener {

    override fun onProfileChanged(view: View, profile: IProfile<*>, current: Boolean): Boolean {
        when (profile.identifier.toInt()) {
            MainActivity.ADD_IDENTITY -> addIdentityDialog()
            MainActivity.MANAGE_IDENTITY -> {
                val identity = Singleton.getIdentity(ctx)
                if (identity == null) {
                    Toast.makeText(ctx, R.string.no_identity_warning, LENGTH_LONG).show()
                } else {
                    val show = Intent(ctx, AddressDetailActivity::class.java)
                    show.putExtra(AddressDetailFragment.ARG_ITEM, identity)
                    ctx.startActivity(show)
                }
            }
            else -> if (profile is ProfileDrawerItem) {
                val tag = profile.tag
                if (tag is BitmessageAddress) {
                    Singleton.setIdentity(tag)
                }
            }
        }
        // false if it should close the drawer
        return false
    }

    private fun addIdentityDialog() {
        AddIdentityDialogFragment().show(fragmentManager, "dialog")
    }
}
