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

package ch.dissem.apps.abit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView

import java.util.ArrayList

import ch.dissem.apps.abit.Identicon
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.entity.BitmessageAddress

/**
 * An adapter for contacts. Can be filtered by alias or address.
 */
class ContactAdapter(ctx: Context) : BaseAdapter(), Filterable {
    private val inflater = LayoutInflater.from(ctx)
    private val originalData = Singleton.getAddressRepository(ctx).getContacts()
    private var data: List<BitmessageAddress> = originalData

    override fun getCount() = data.size

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder = if (convertView == null) {
            ViewHolder(inflater.inflate(R.layout.contact_row, parent, false))
        } else {
            convertView.tag as ViewHolder
        }
        val item = getItem(position)
        viewHolder.avatar.setImageDrawable(Identicon(item))
        viewHolder.name.text = item.toString()
        viewHolder.address.text = item.address
        return viewHolder.view
    }

    override fun getFilter(): Filter = ContactFilter()

    private inner class ViewHolder(val view: View) {
        val avatar = view.findViewById<ImageView>(R.id.avatar)!!
        val name = view.findViewById<TextView>(R.id.name)!!
        val address = view.findViewById<TextView>(R.id.address)!!

        init {
            view.tag = this
        }
    }


    private inner class ContactFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): Filter.FilterResults {
            val results = Filter.FilterResults()

            if (prefix?.isEmpty() == false) {
                val prefixString = prefix.toString().toLowerCase()

                val newValues = ArrayList<BitmessageAddress>()

                originalData
                        .forEach { value ->
                            value.alias?.toLowerCase()?.let { alias ->
                                if (alias.startsWith(prefixString)) {
                                    newValues.add(value)
                                } else {
                                    val words = alias.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                                    for (word in words) {
                                        if (word.startsWith(prefixString)) {
                                            newValues.add(value)
                                            break
                                        }
                                    }
                                }
                            } ?: {
                                val address = value.address.toLowerCase()
                                if (address.contains(prefixString)) {
                                    newValues.add(value)
                                }
                            }.invoke()
                        }

                results.values = newValues
                results.count = newValues.size
            } else {
                results.values = originalData
                results.count = originalData.size
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
            @Suppress("UNCHECKED_CAST")
            data = results.values as List<BitmessageAddress>
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}
