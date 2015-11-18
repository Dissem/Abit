package ch.dissem.apps.abit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * An adapter for contacts. Can be filtered by alias or address.
 */
public class ContactAdapter extends BaseAdapter implements Filterable {
    private final LayoutInflater inflater;
    private final List<BitmessageAddress> originalData;
    private List<BitmessageAddress> data;

    public ContactAdapter(Context ctx) {
        inflater = LayoutInflater.from(ctx);
        originalData = Singleton.getAddressRepository(ctx).getContacts();
        data = originalData;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public BitmessageAddress getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.contact_row, parent, false);
        }
        BitmessageAddress item = getItem(position);
        ((ImageView) convertView.findViewById(R.id.avatar)).setImageDrawable(new Identicon(item));
        ((TextView) convertView.findViewById(R.id.name)).setText(item.toString());
        ((TextView) convertView.findViewById(R.id.address)).setText(item.getAddress());
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new ContactFilter();
    }

    private class ContactFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (prefix == null || prefix.length() == 0) {
                results.values = originalData;
                results.count = originalData.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();

                final ArrayList<BitmessageAddress> newValues = new ArrayList<>();

                for (int i = 0; i < originalData.size(); i++) {
                    final BitmessageAddress value = originalData.get(i);

                    // First match against the whole, non-splitted value
                    if (value.getAlias() != null) {
                        String alias = value.getAlias().toLowerCase();
                        if (alias.startsWith(prefixString)) {
                            newValues.add(value);
                        } else {
                            final String[] words = alias.split(" ");

                            for (String word : words) {
                                if (word.startsWith(prefixString)) {
                                    newValues.add(value);
                                    break;
                                }
                            }
                        }
                    } else {
                        String address = value.getAddress().toLowerCase();
                        if (address.contains(prefixString)) {
                            newValues.add(value);
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            data = (List<BitmessageAddress>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
