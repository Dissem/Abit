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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.valueobject.Label;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

/**
 * Fragment that shows a list of all contacts, the ones we subscribed to first.
 */
public class AddressListFragment extends AbstractItemListFragment<BitmessageAddress> {
    @Override
    public void onResume() {
        super.onResume();

        updateList();
    }

    public void updateList() {
        List<BitmessageAddress> addresses = Singleton.getAddressRepository(getContext())
                .getContacts();
        Collections.sort(addresses, new Comparator<BitmessageAddress>() {
            /**
             * Yields the following order:
             * <ol>
             *     <li>Subscribed addresses come first</li>
             *     <li>Addresses with Aliases (alphabetically)</li>
             *     <li>Addresses (alphabetically)</li>
             * </ol>
             */
            @Override
            public int compare(BitmessageAddress lhs, BitmessageAddress rhs) {
                if (lhs.isSubscribed() == rhs.isSubscribed()) {
                    if (lhs.getAlias() != null) {
                        if (rhs.getAlias() != null) {
                            return lhs.getAlias().compareTo(rhs.getAlias());
                        } else {
                            return -1;
                        }
                    } else if (rhs.getAlias() != null) {
                        return 1;
                    } else {
                        return lhs.getAddress().compareTo(rhs.getAddress());
                    }
                }
                if (lhs.isSubscribed()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        setListAdapter(new ArrayAdapter<BitmessageAddress>(
                getActivity(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                addresses) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.subscription_row, null, false);
                }
                BitmessageAddress item = getItem(position);
                ((ImageView) convertView.findViewById(R.id.avatar)).setImageDrawable(new
                        Identicon(item));
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(item.toString());
                TextView streamNumber = (TextView) convertView.findViewById(R.id.stream_number);
                streamNumber.setText(getContext().getString(R.string.stream_number, item
                        .getStream()));
                convertView.findViewById(R.id.subscribed).setVisibility(item.isSubscribed() ?
                        View.VISIBLE : View.INVISIBLE);
                return convertView;
            }
        });
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof ActionBarListener) {
            ((ActionBarListener) ctx).updateTitle(getString(R.string.contacts_and_subscriptions));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_address_list, container, false);

        FabSpeedDial fabSpeedDial = (FabSpeedDial) view.findViewById(R.id.fab_add_contact);
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_read_qr_code:
                        IntentIntegrator.forSupportFragment(AddressListFragment.this)
                                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                                .initiateScan();
                        return true;
                    case R.id.action_create_contact:
                        Intent intent = new Intent(getActivity(), CreateAddressActivity.class);
                        startActivity(intent);
                        return true;
                    default:
                        return false;
                }
            }
        });

        return view;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            Uri uri = Uri.parse(data.getStringExtra("SCAN_RESULT"));
            Intent intent = new Intent(getActivity(), CreateAddressActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    @Override
    void updateList(Label label) {
        updateList();
    }
}
