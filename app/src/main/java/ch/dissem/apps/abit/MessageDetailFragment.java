package ch.dissem.apps.abit;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import java.util.Iterator;


/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a {@link MessageListActivity}
 * in two-pane mode (on tablets) or a {@link MessageDetailActivity}
 * on handsets.
 */
public class MessageDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM = "item";

    /**
     * The dummy content this fragment is presenting.
     */
    private Plaintext item;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            item = (Plaintext) getArguments().getSerializable(ARG_ITEM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (item != null) {
            ((TextView) rootView.findViewById(R.id.subject)).setText(item.getSubject());
            BitmessageAddress sender = item.getFrom();
            ((ImageView) rootView.findViewById(R.id.avatar)).setImageDrawable(new Identicon(sender));
            ((TextView) rootView.findViewById(R.id.sender)).setText(sender.toString());
            if (item.getTo() != null) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(item.getTo().toString());
            } else if (item.getType() == Plaintext.Type.BROADCAST) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(R.string.broadcast);
            }
            ((TextView) rootView.findViewById(R.id.text)).setText(item.getText());
        }

        boolean removed = false;
        Iterator<Label> labels = item.getLabels().iterator();
        while (labels.hasNext()) {
            if (labels.next().getType() == Label.Type.UNREAD) {
                labels.remove();
                removed = true;
            }
        }
        if (removed) {
            Singleton.getBitmessageContext(inflater.getContext()).messages().save(item);
        }
        return rootView;
    }
}
