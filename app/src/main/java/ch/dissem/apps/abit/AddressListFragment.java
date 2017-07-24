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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.LinkedList;
import java.util.List;

import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;

/**
 * Fragment that shows a list of all contacts, the ones we subscribed to first.
 */
public class AddressListFragment extends AbstractItemListFragment<Void, BitmessageAddress> {
    private ArrayAdapter<BitmessageAddress> adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<BitmessageAddress>(
            getActivity(),
            R.layout.subscription_row,
            R.id.name,
            new LinkedList<BitmessageAddress>()) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                ViewHolder v;
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.subscription_row, parent, false);
                    v = new ViewHolder();
                    v.ctx = getContext();
                    v.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    v.name = (TextView) convertView.findViewById(R.id.name);
                    v.streamNumber = (TextView) convertView.findViewById(R.id.stream_number);
                    v.subscribed = convertView.findViewById(R.id.subscribed);
                    convertView.setTag(v);
                } else {
                    v = (ViewHolder) convertView.getTag();
                }
                BitmessageAddress item = getItem(position);
                assert item != null;
                v.avatar.setImageDrawable(new Identicon(item));
                v.name.setText(item.toString());
                v.streamNumber.setText(v.ctx.getString(R.string.stream_number, item.getStream()));
                v.subscribed.setVisibility(item.isSubscribed() ? View.VISIBLE : View.INVISIBLE);
                return convertView;
            }
        };
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateList();
    }

    public void updateList() {
        adapter.clear();
        final AndroidAddressRepository addressRepo = Singleton.getAddressRepository(getContext());
        new AsyncTask<Void, BitmessageAddress, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<String> ids = addressRepo.getContactIds();
                for (String id : ids) {
                    BitmessageAddress address = addressRepo.getById(id);
                    publishProgress(address);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(BitmessageAddress... values) {
                for (BitmessageAddress address : values) {
                    adapter.add(address);
                }
            }
        }.execute();
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
        fabSpeedDial.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton floatingActionButton, @Nullable TextView textView, int itemId) {
                switch (itemId) {
                    case R.id.action_read_qr_code:
                        IntentIntegrator.forSupportFragment(AddressListFragment.this)
                            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                            .initiateScan();
                        break;
                    case R.id.action_create_contact:
                        Intent intent = new Intent(getActivity(), CreateAddressActivity.class);
                        startActivity(intent);
                        break;
                    default:
                        break;
                }
            }
        });

        return view;
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
    public void updateList(Void label) {
        updateList();
    }

    private static class ViewHolder {
        private Context ctx;
        private ImageView avatar;
        private TextView name;
        private TextView streamNumber;
        private View subscribed;
    }
}
