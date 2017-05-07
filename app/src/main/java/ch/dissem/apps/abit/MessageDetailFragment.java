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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.view.IconicsImageView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Assets;
import ch.dissem.apps.abit.util.Drawables;
import ch.dissem.apps.abit.util.Labels;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.ports.MessageRepository;

import static android.text.util.Linkify.WEB_URLS;
import static ch.dissem.apps.abit.util.Constants.BITMESSAGE_ADDRESS_PATTERN;
import static ch.dissem.apps.abit.util.Constants.BITMESSAGE_URL_SCHEMA;
import static ch.dissem.apps.abit.util.Strings.prepareMessageExtract;


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
            ImageView status = (ImageView) rootView.findViewById(R.id.status);
            status.setImageResource(Assets.getStatusDrawable(item.getStatus()));
            status.setContentDescription(getString(Assets.getStatusString(item.getStatus())));
            BitmessageAddress sender = item.getFrom();
            ((ImageView) rootView.findViewById(R.id.avatar))
                .setImageDrawable(new Identicon(sender));
            ((TextView) rootView.findViewById(R.id.sender)).setText(sender.toString());
            if (item.getTo() != null) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(item.getTo().toString());
            } else if (item.getType() == Plaintext.Type.BROADCAST) {
                ((TextView) rootView.findViewById(R.id.recipient)).setText(R.string.broadcast);
            }
            RecyclerView labelView = (RecyclerView) rootView.findViewById(R.id.labels);
            LabelAdapter labelAdapter = new LabelAdapter(getActivity(), item.getLabels());
            labelView.setAdapter(labelAdapter);
            labelView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

            TextView messageBody = (TextView) rootView.findViewById(R.id.text);
            messageBody.setText(item.getText());

            Linkify.addLinks(messageBody, WEB_URLS);
            Linkify.addLinks(messageBody, BITMESSAGE_ADDRESS_PATTERN, BITMESSAGE_URL_SCHEMA, null,
                new Linkify.TransformFilter() {
                    @Override
                    public String transformUrl(Matcher match, String url) {
                        return match.group();
                    }
                }
            );

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
            MessageRepository messageRepo = Singleton.getMessageRepository(inflater.getContext());
            if (removed) {
                if (getActivity() instanceof ActionBarListener) {
                    ((ActionBarListener) getActivity()).updateUnread();
                }
                messageRepo.save(item);
            }
            List<Plaintext> parents = new ArrayList<>(item.getParents().size());
            for (InventoryVector parentIV : item.getParents()) {
                Plaintext parent = messageRepo.getMessage(parentIV);
                if (parent != null) {
                    parents.add(parent);
                }
            }
            showRelatedMessages(rootView, R.id.parents, parents);
            showRelatedMessages(rootView, R.id.responses, messageRepo.findResponses(item));
        }
        return rootView;
    }

    private void showRelatedMessages(View rootView, @IdRes int id, List<Plaintext> messages) {
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(id);
        RelatedMessageAdapter adapter = new RelatedMessageAdapter(getActivity(), messages);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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
                ComposeMessageActivity.launchReplyTo(this, item);
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
                if (getActivity() instanceof ActionBarListener) {
                    ((ActionBarListener) getActivity()).updateUnread();
                }
                return true;
            case R.id.archive:
                if (item.isUnread() && getActivity() instanceof ActionBarListener) {
                    ((ActionBarListener) getActivity()).updateUnread();
                }
                item.getLabels().clear();
                messageRepo.save(item);
                return true;
            default:
                return false;
        }
    }

    public static boolean isInTrash(Plaintext item) {
        for (Label label : item.getLabels()) {
            if (label.getType() == Label.Type.TRASH) {
                return true;
            }
        }
        return false;
    }

    private static class RelatedMessageAdapter extends RecyclerView.Adapter<RelatedMessageAdapter.ViewHolder> {
        private final List<Plaintext> messages;
        private final Context ctx;

        private RelatedMessageAdapter(Context ctx, List<Plaintext> messages) {
            this.messages = messages;
            this.ctx = ctx;
        }

        @Override
        public RelatedMessageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View contactView = inflater.inflate(R.layout.item_message_minimized, parent, false);

            // Return a new holder instance
            return new RelatedMessageAdapter.ViewHolder(contactView);
        }

        // Involves populating data into the item through holder
        @Override
        public void onBindViewHolder(RelatedMessageAdapter.ViewHolder viewHolder, int position) {
            // Get the data model based on position
            Plaintext message = messages.get(position);

            viewHolder.avatar.setImageDrawable(new Identicon(message.getFrom()));
            viewHolder.status.setImageResource(Assets.getStatusDrawable(message.getStatus()));
            viewHolder.sender.setText(message.getFrom().toString());
            viewHolder.extract.setText(prepareMessageExtract(message.getText()));
            viewHolder.item = message;
        }

        // Returns the total count of items in the list
        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView avatar;
            private final ImageView status;
            private final TextView sender;
            private final TextView extract;
            private Plaintext item;

            ViewHolder(final View itemView) {
                super(itemView);
                avatar = (ImageView) itemView.findViewById(R.id.avatar);
                status = (ImageView) itemView.findViewById(R.id.status);
                sender = (TextView) itemView.findViewById(R.id.sender);
                extract = (TextView) itemView.findViewById(R.id.text);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ctx instanceof MainActivity) {
                            ((MainActivity) ctx).onItemSelected(item);
                        } else {
                            Intent detailIntent;
                            detailIntent = new Intent(ctx, MessageDetailActivity.class);
                            detailIntent.putExtra(MessageDetailFragment.ARG_ITEM, item);
                            ctx.startActivity(detailIntent);
                        }
                    }
                });

            }
        }
    }

    private static class LabelAdapter extends
        RecyclerView.Adapter<LabelAdapter.ViewHolder> {

        private final List<Label> labels;
        private final Context ctx;

        private LabelAdapter(Context ctx, Set<Label> labels) {
            this.labels = new ArrayList<>(labels);
            this.ctx = ctx;
        }

        @Override
        public LabelAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View contactView = inflater.inflate(R.layout.item_label, parent, false);

            // Return a new holder instance
            return new ViewHolder(contactView);
        }

        // Involves populating data into the item through holder
        @Override
        public void onBindViewHolder(LabelAdapter.ViewHolder viewHolder, int position) {
            // Get the data model based on position
            Label label = labels.get(position);

            viewHolder.icon.setColor(Labels.getColor(label));
            viewHolder.icon.setIcon(Labels.getIcon(label));
            viewHolder.label.setText(Labels.getText(label, ctx));
        }

        // Returns the total count of items in the list
        @Override
        public int getItemCount() {
            return labels.size();
        }

        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        static class ViewHolder extends RecyclerView.ViewHolder {
            // Your holder should contain a member variable
            // for any view that will be set as you render a row
            public IconicsImageView icon;
            public TextView label;

            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            ViewHolder(View itemView) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);

                icon = (IconicsImageView) itemView.findViewById(R.id.icon);
                label = (TextView) itemView.findViewById(R.id.label);
            }
        }
    }
}
