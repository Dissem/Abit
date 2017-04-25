package ch.dissem.apps.abit.drawer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import ch.dissem.apps.abit.AddressDetailActivity;
import ch.dissem.apps.abit.AddressDetailFragment;
import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.dialog.AddIdentityDialogFragment;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;

import static android.widget.Toast.LENGTH_LONG;

public class ProfileSelectionListener implements AccountHeader.OnAccountHeaderListener {
    private final Context ctx;
    private final FragmentManager fragmentManager;

    public ProfileSelectionListener(Context ctx, FragmentManager fragmentManager) {
        this.ctx = ctx;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        switch ((int) profile.getIdentifier()) {
            case MainActivity.ADD_IDENTITY:
                addIdentityDialog();
                break;
            case MainActivity.MANAGE_IDENTITY:
                BitmessageAddress identity = Singleton.getIdentity(ctx);
                if (identity == null) {
                    Toast.makeText(ctx, R.string.no_identity_warning, LENGTH_LONG).show();
                } else {
                    Intent show = new Intent(ctx, AddressDetailActivity.class);
                    show.putExtra(AddressDetailFragment.ARG_ITEM, identity);
                    ctx.startActivity(show);
                }
                break;
            default:
                if (profile instanceof ProfileDrawerItem) {
                    Object tag = ((ProfileDrawerItem) profile).getTag();
                    if (tag instanceof BitmessageAddress) {
                        Singleton.setIdentity((BitmessageAddress) tag);
                    }
                }
                break;
        }
        // false if it should close the drawer
        return false;
    }

    private void addIdentityDialog() {
        AddIdentityDialogFragment dialog = new AddIdentityDialogFragment();
        dialog.show(fragmentManager, "dialog");
    }
}
