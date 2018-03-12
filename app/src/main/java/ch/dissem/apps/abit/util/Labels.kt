package ch.dissem.apps.abit.util

import android.content.Context
import android.support.annotation.ColorInt

import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon

import ch.dissem.apps.abit.R
import ch.dissem.bitmessage.entity.valueobject.Label

/*
 * Helper methods to help with translating the default labels, getting label colors and so on.
 */

fun Label.getText(ctx: Context): String = Labels.getText(type, toString(), ctx)!!

object Labels {
    fun getText(type: Label.Type?, alternative: String?, ctx: Context) = when (type) {
        Label.Type.INBOX -> ctx.getString(R.string.inbox)
        Label.Type.DRAFT -> ctx.getString(R.string.draft)
        Label.Type.OUTBOX -> ctx.getString(R.string.outbox)
        Label.Type.SENT -> ctx.getString(R.string.sent)
        Label.Type.UNREAD -> ctx.getString(R.string.unread)
        Label.Type.TRASH -> ctx.getString(R.string.trash)
        Label.Type.BROADCAST -> ctx.getString(R.string.broadcasts)
        else -> alternative
    }
}

fun Label.getIcon(): IIcon = when (type) {
    Label.Type.INBOX -> GoogleMaterial.Icon.gmd_inbox
    Label.Type.DRAFT -> CommunityMaterial.Icon.cmd_file
    Label.Type.OUTBOX -> CommunityMaterial.Icon.cmd_inbox_arrow_up
    Label.Type.SENT -> CommunityMaterial.Icon.cmd_send
    Label.Type.BROADCAST -> CommunityMaterial.Icon.cmd_rss
    Label.Type.UNREAD -> GoogleMaterial.Icon.gmd_markunread_mailbox
    Label.Type.TRASH -> GoogleMaterial.Icon.gmd_delete
    else -> CommunityMaterial.Icon.cmd_label
}

@ColorInt
fun Label.getColor(@ColorInt default: Int) = if (type == null) {
    color
} else default
