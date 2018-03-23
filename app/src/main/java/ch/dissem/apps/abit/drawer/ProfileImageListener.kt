package ch.dissem.apps.abit.drawer

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.qrCode
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.model.interfaces.IProfile

class ProfileImageListener(private val ctx: Context) : AccountHeader.OnAccountHeaderProfileImageListener {

    override fun onProfileImageClick(view: View, profile: IProfile<*>, current: Boolean): Boolean {
        if (current) {
            //  Show QR code in modal dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val imageView = ImageView(ctx)
            imageView.setImageBitmap(Singleton.getIdentity(ctx)?.qrCode())
            imageView.setOnClickListener { dialog.dismiss() }
            dialog.addContentView(
                    imageView,
                    RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            )
            val window = dialog.window
            if (window != null) {
                val display = window.windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                val dim = if (size.x < size.y) size.x else size.y

                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = dim
                lp.height = dim

                window.attributes = lp
            }
            dialog.show()
            return true
        }
        return false
    }

    override fun onProfileImageLongClick(view: View, iProfile: IProfile<*>, b: Boolean) = false
}
