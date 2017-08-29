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

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import ch.dissem.apps.abit.R
import ch.dissem.bitmessage.entity.BitmessageAddress
import java.util.*

/**
 * @author Christian Basler
 */
class AddressSelectorAdapter(identities: List<BitmessageAddress>) : RecyclerView.Adapter<AddressSelectorAdapter.ViewHolder>() {

    private val data = identities.map { Selectable(it) }.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.select_identity_row, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectable = data[position]
        holder.data = selectable
        holder.checkbox.isChecked = selectable.selected
        holder.checkbox.text = selectable.data.toString()
        holder.address.text = selectable.data.address
    }

    override fun getItemCount() = data.size

    class ViewHolder internal constructor(v: View) : RecyclerView.ViewHolder(v) {
        var data: Selectable<BitmessageAddress>? = null
        val checkbox = v.findViewById(R.id.checkbox) as CheckBox
        val address = v.findViewById(R.id.address) as TextView

        init {
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                data?.selected = isChecked
            }
        }
    }

    val selected: List<BitmessageAddress>
        get() {
            val result = LinkedList<BitmessageAddress>()
            for (selectable in data) {
                if (selectable.selected) {
                    result.add(selectable.data)
                }
            }
            return result
        }
}
