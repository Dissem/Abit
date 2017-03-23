package ch.dissem.apps.abit.util;

import android.content.Context;
import android.support.annotation.ColorInt;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.typeface.IIcon;

import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.valueobject.Label;

/**
 * Helper class to help with translating the default labels, getting label colors and so on.
 */
public class Labels {
    public static String getText(Label label, Context ctx) {
        return getText(label.getType(), label.toString(), ctx);
    }

    public static String getText(Label.Type type, String alternative, Context ctx) {
        if (type == null) {
            return alternative;
        } else {
            switch (type) {
                case INBOX:
                    return ctx.getString(R.string.inbox);
                case DRAFT:
                    return ctx.getString(R.string.draft);
                case OUTBOX:
                    return ctx.getString(R.string.outbox);
                case SENT:
                    return ctx.getString(R.string.sent);
                case UNREAD:
                    return ctx.getString(R.string.unread);
                case TRASH:
                    return ctx.getString(R.string.trash);
                case BROADCAST:
                    return ctx.getString(R.string.broadcasts);
                default:
                    return alternative;
            }
        }
    }

    public static IIcon getIcon(Label label) {
        if (label.getType() == null) {
            return CommunityMaterial.Icon.cmd_label;
        }
        switch (label.getType()) {
            case INBOX:
                return GoogleMaterial.Icon.gmd_inbox;
            case DRAFT:
                return CommunityMaterial.Icon.cmd_file;
            case OUTBOX:
                return CommunityMaterial.Icon.cmd_inbox_arrow_up;
            case SENT:
                return CommunityMaterial.Icon.cmd_send;
            case BROADCAST:
                return CommunityMaterial.Icon.cmd_rss;
            case UNREAD:
                return GoogleMaterial.Icon.gmd_markunread_mailbox;
            case TRASH:
                return GoogleMaterial.Icon.gmd_delete;
            default:
                return CommunityMaterial.Icon.cmd_label;
        }
    }

    @ColorInt
    public static int getColor(Label label) {
        if (label.getType() == null) {
            return label.getColor();
        }
        return 0xFF000000;
    }
}
