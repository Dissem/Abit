package ch.dissem.apps.abit.drawer;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Drawables;

public class ProfileImageListener implements AccountHeader.OnAccountHeaderProfileImageListener {
    private final Context ctx;

    public ProfileImageListener(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean onProfileImageClick(View view, IProfile profile, boolean current) {
        if (current) {
            //  Show QR code in modal dialog
            final Dialog dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            ImageView imageView = new ImageView(ctx);
            imageView.setImageBitmap(Drawables.qrCode(Singleton.getIdentity(ctx)));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            Window window = dialog.getWindow();
            if (window != null) {
                Display display = window.getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int dim = size.x < size.y ? size.x : size.y;

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(window.getAttributes());
                lp.width = dim;
                lp.height = dim;

                window.setAttributes(lp);
            }
            dialog.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onProfileImageLongClick(View view, IProfile iProfile, boolean b) {
        return false;
    }
}
