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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;

import java.io.IOException;

import ch.dissem.apps.abit.adapter.AddressSelectorAdapter;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.wif.WifImporter;

/**
 * @author Christian Basler
 */

public class ImportIdentitiesFragment extends Fragment {
    public static final String WIF_DATA = "wif_data";
    private BitmessageContext bmc;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private AddressSelectorAdapter adapter;
    private WifImporter importer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
        savedInstanceState) {
        String wifData = getArguments().getString(WIF_DATA);
        bmc = Singleton.getBitmessageContext(getActivity());
        View view = inflater.inflate(R.layout.fragment_import_select_identities, container, false);
        try {
            importer = new WifImporter(bmc, wifData);
            adapter = new AddressSelectorAdapter(importer.getIdentities());
            layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL,
                false);
            recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);

            recyclerView.addItemDecoration(new SimpleListDividerDecorator(
                ContextCompat.getDrawable(getActivity(), R.drawable.list_divider_h), true));
        } catch (IOException e) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
        view.findViewById(R.id.finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importer.importAll(adapter.getSelected());
                MainActivity mainActivity = MainActivity.getInstance();
                if (mainActivity != null) {
                    for (BitmessageAddress selected : adapter.getSelected()) {
                        mainActivity.addIdentityEntry(selected);
                    }
                }
                getActivity().finish();
            }
        });
        return view;
    }
}
