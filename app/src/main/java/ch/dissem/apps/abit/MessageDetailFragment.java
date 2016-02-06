/*
 * Copyright 2016 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import java.util.Iterator;
import java.util.regex.Matcher;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Drawables;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.ports.MessageRepository;

import static android.text.util.Linkify.WEB_URLS;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_IDENTITY;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_RECIPIENT;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_SUBJECT;
import static ch.dissem.apps.abit.util.Constants.BITMESSAGE_ADDRESS_PATTERN;
import static ch.dissem.apps.abit.util.Constants.BITMESSAGE_URL_SCHEMA;


/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a {@link MainActivity}
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
     * The content this fragment is presenting.
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
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (item != null) {
            ((TextView) rootView.findViewById(R.id.subject)).setText(item.getSubject());
            BitmessageAddress sender = item.getFrom();
            ((ImageView) rootView.findViewById(R.id.avatar)).setImageDrawable(new Identicon
                    (sender));
            ((TextView) rootView.findViewById(R.id.sender)).setText(sender.toString());
            if (item.getTo() != null) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(item.getTo().toString());
            } else if (item.getType() == Plaintext.Type.BROADCAST) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(R.string.broadcast);
            }
            TextView messageBody = (TextView) rootView.findViewById(R.id.text);
            messageBody.setText(item.getText());

            Linkify.addLinks(messageBody, WEB_URLS);
            Linkify.addLinks(messageBody, BITMESSAGE_ADDRESS_PATTERN, BITMESSAGE_URL_SCHEMA, null,
                    new TransformFilter() {
                        public final String transformUrl(final Matcher match, String url) {
                            return match.group();
                        }
                    });

            messageBody.setLinksClickable(true);
            messageBody.setTextIsSelectable(true);

            boolean removed = false;
            Iterator<Label> labels = item.getLabels().iterator();
            while (labels.hasNext()) {
                if (labels.next().getType() == Label.Type.UNREAD) {
                    labels.remove();
                    removed = true;
                }
            }
            if (removed) {
                Singleton.getMessageRepository(inflater.getContext()).save(item);
            }
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message, menu);

        Drawables.addIcon(getActivity(), menu, R.id.reply, GoogleMaterial.Icon.gmd_reply);
        Drawables.addIcon(getActivity(), menu, R.id.delete, GoogleMaterial.Icon.gmd_delete);
        Drawables.addIcon(getActivity(), menu, R.id.mark_unread, GoogleMaterial.Icon
                .gmd_markunread);
        Drawables.addIcon(getActivity(), menu, R.id.archive, GoogleMaterial.Icon.gmd_archive);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        MessageRepository messageRepo = Singleton.getMessageRepository(getContext());
        switch (menuItem.getItemId()) {
            case R.id.reply:
                Intent replyIntent = new Intent(getActivity().getApplicationContext(),
                        ComposeMessageActivity.class);
                replyIntent.putExtra(EXTRA_RECIPIENT, item.getFrom());
                replyIntent.putExtra(EXTRA_IDENTITY, item.getTo());
                String prefix;
                if (item.getSubject().length() >= 3 && item.getSubject().substring(0, 3)
                        .equalsIgnoreCase("RE:")) {
                    prefix = "";
                } else {
                    prefix = "RE: ";
                }
                replyIntent.putExtra(EXTRA_SUBJECT, prefix + item.getSubject());
                startActivity(replyIntent);
                return true;
            case R.id.delete:
                if (isInTrash(item)) {
                    messageRepo.remove(item);
                } else {
                    item.getLabels().clear();
                    item.addLabels(messageRepo.getLabels(Label.Type.TRASH));
                    messageRepo.save(item);
                }
                getActivity().onBackPressed();
                return true;
            case R.id.mark_unread:
                item.addLabels(messageRepo.getLabels(Label.Type.UNREAD));
                messageRepo.save(item);
                return true;
            case R.id.archive:
                item.getLabels().clear();
                messageRepo.save(item);
                return true;
            default:
                return false;
        }
    }

    private boolean isInTrash(Plaintext item) {
        for (Label label : item.getLabels()) {
            if (label.getType() == Label.Type.TRASH) {
                return true;
            }
        }
        return false;
    }
}
