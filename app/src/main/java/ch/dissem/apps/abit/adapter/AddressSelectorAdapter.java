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

package ch.dissem.apps.abit.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * @author Christian Basler
 */

public class AddressSelectorAdapter
    extends RecyclerView.Adapter<AddressSelectorAdapter.ViewHolder> {

    private final List<Selectable<BitmessageAddress>> data;

    public AddressSelectorAdapter(List<BitmessageAddress> identities) {
        data = new ArrayList<>(identities.size());
        for (BitmessageAddress identity : identities) {
            data.add(new Selectable<>(identity));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.select_identity_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Selectable<BitmessageAddress> selectable = data.get(position);
        holder.data = selectable;
        holder.checkbox.setChecked(selectable.selected);
        holder.checkbox.setText(selectable.data.toString());
        holder.address.setText(selectable.data.getAddress());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public Selectable<BitmessageAddress> data;
        public CheckBox checkbox;
        public TextView address;

        private ViewHolder(View v) {
            super(v);
            checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            address = (TextView) v.findViewById(R.id.address);
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (data != null) {
                        data.selected = isChecked;
                    }
                }
            });
        }
    }

    private static class Selectable<T> {
        private final T data;
        private boolean selected = false;

        private Selectable(T data) {
            this.data = data;
        }
    }

    public List<BitmessageAddress> getSelected() {
        List<BitmessageAddress> result = new LinkedList<>();
        for (Selectable<BitmessageAddress> selectable : data) {
            if (selectable.selected) {
                result.add(selectable.data);
            }
        }
        return result;
    }
}
