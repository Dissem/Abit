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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;


/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a {@link MessageListActivity}
 * in two-pane mode (on tablets) or a {@link MessageDetailActivity}
 * on handsets.
 */
public class SubscriptionDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM = "item";

    /**
     * The content this fragment is presenting.
     */
    private BitmessageAddress item;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubscriptionDetailFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_subscription_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (item != null) {
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
            ((TextView) rootView.findViewById(R.id.stream_number)).setText(getActivity().getString(R.string.stream_number, item.getStream()));
            Switch active = (Switch) rootView.findViewById(R.id.active);
            active.setChecked(item.isSubscribed());
            active.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setSubscribed(isChecked);
                }
            });
        }

        return rootView;
    }

    @Override
    public void onPause() {
        Singleton.getAddressRepository(getContext()).save(item);
        super.onPause();
    }
}
