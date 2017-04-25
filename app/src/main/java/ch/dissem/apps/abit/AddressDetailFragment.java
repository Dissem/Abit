/*
 * Copyright 2015 Christian Basler
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Drawables;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.wif.WifExporter;


/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a {@link MainActivity}
 * in two-pane mode (on tablets) or a {@link MessageDetailActivity}
 * on handsets.
 */
public class AddressDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM = "item";
    public static final String EXPORT_POSTFIX = ".keys.dat";

    /**
     * The content this fragment is presenting.
     */
    private BitmessageAddress item;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AddressDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            item = (BitmessageAddress) getArguments().getSerializable(ARG_ITEM);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.address, menu);

        FragmentActivity activity = getActivity();
        Drawables.addIcon(activity, menu, R.id.write_message, GoogleMaterial.Icon.gmd_mail);
        Drawables.addIcon(activity, menu, R.id.share, GoogleMaterial.Icon.gmd_share);
        Drawables.addIcon(activity, menu, R.id.delete, GoogleMaterial.Icon.gmd_delete);
        Drawables.addIcon(activity, menu, R.id.export,
            CommunityMaterial.Icon.cmd_export)
            .setVisible(item != null && item.getPrivateKey() != null);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        final Activity ctx = getActivity();
        switch (menuItem.getItemId()) {
            case R.id.write_message: {
                BitmessageAddress identity = Singleton.getIdentity(ctx);
                if (identity == null) {
                    Toast.makeText(ctx, R.string.no_identity_warning, Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(ctx, ComposeMessageActivity.class);
                    intent.putExtra(ComposeMessageActivity.EXTRA_IDENTITY, identity);
                    intent.putExtra(ComposeMessageActivity.EXTRA_RECIPIENT, item);
                    startActivity(intent);
                }
                return true;
            }
            case R.id.delete: {
                int warning;
                if (item.getPrivateKey() != null)
                    warning = R.string.delete_identity_warning;
                else
                    warning = R.string.delete_contact_warning;
                new AlertDialog.Builder(ctx)
                    .setMessage(warning)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Singleton.getAddressRepository(ctx).remove(item);
                            MainActivity mainActivity = MainActivity.getInstance();
                            if (item.getPrivateKey() != null && mainActivity != null) {
                                mainActivity.removeIdentityEntry(item);
                            }
                            item = null;
                            ctx.onBackPressed();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
                return true;
            }
            case R.id.export: {
                new AlertDialog.Builder(ctx)
                    .setMessage(R.string.confirm_export)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TITLE, item +
                                EXPORT_POSTFIX);
                            WifExporter exporter = new WifExporter(Singleton
                                .getBitmessageContext(ctx));
                            exporter.addIdentity(item);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, exporter.toString
                                ());
                            startActivity(Intent.createChooser(shareIntent, null));
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
                return true;
            }
            case R.id.share: {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, item.getAddress());
                startActivity(Intent.createChooser(shareIntent, null));
            }
            default:
                return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_address_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (item != null) {
            FragmentActivity activity = getActivity();
            if (item.isChan()) {
                activity.setTitle(R.string.title_chan_detail);
            } else if (item.getPrivateKey() != null) {
                activity.setTitle(R.string.title_identity_detail);
            } else if (item.isSubscribed()) {
                activity.setTitle(R.string.title_subscription_detail);
            } else {
                activity.setTitle(R.string.title_contact_detail);
            }

            ((ImageView) rootView.findViewById(R.id.avatar)).setImageDrawable(new Identicon(item));
            TextView name = (TextView) rootView.findViewById(R.id.name);
            name.setText(item.toString());
            name.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Nothing to do
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Nothing to do
                }

                @Override
                public void afterTextChanged(Editable s) {
                    item.setAlias(s.toString());
                }
            });
            TextView address = (TextView) rootView.findViewById(R.id.address);
            address.setText(item.getAddress());
            address.setSelected(true);
            ((TextView) rootView.findViewById(R.id.stream_number)).setText(
                getString(R.string.stream_number, item.getStream()));
            if (item.getPrivateKey() == null) {
                Switch active = (Switch) rootView.findViewById(R.id.active);
                active.setChecked(item.isSubscribed());
                active.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean checked) {
                        item.setSubscribed(checked);
                    }
                });

                ImageView pubkeyAvailableImg = (ImageView) rootView.findViewById(R.id
                    .pubkey_available);

                if (item.getPubkey() == null) {
                    pubkeyAvailableImg.setAlpha(0.3f);
                    TextView pubkeyAvailableDesc = (TextView) rootView.findViewById(R.id
                        .pubkey_available_desc);
                    pubkeyAvailableDesc.setText(R.string.pubkey_not_available);
                }
            } else {
                rootView.findViewById(R.id.active).setVisibility(View.GONE);
                rootView.findViewById(R.id.pubkey_available).setVisibility(View.GONE);
                rootView.findViewById(R.id.pubkey_available_desc).setVisibility(View.GONE);
            }

            // QR code
            ImageView qrCode = (ImageView) rootView.findViewById(R.id.qr_code);
            qrCode.setImageBitmap(Drawables.qrCode(item));
        }

        return rootView;
    }

    @Override
    public void onPause() {
        if (item != null) {
            Singleton.getAddressRepository(getContext()).save(item);
            MainActivity mainActivity = MainActivity.getInstance();
            if (mainActivity != null && item.getPrivateKey() != null) {
                mainActivity.updateIdentityEntry(item);
            }
        }
        super.onPause();
    }
}
