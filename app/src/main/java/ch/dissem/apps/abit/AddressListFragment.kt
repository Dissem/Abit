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

package ch.dissem.apps.abit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.FabUtils
import ch.dissem.bitmessage.entity.BitmessageAddress
import com.google.zxing.integration.android.IntentIntegrator
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

/**
 * Fragment that shows a list of all contacts, the ones we subscribed to first.
 */
class AddressListFragment : AbstractItemListFragment<Void, BitmessageAddress>() {
    private lateinit var adapter: ArrayAdapter<BitmessageAddress>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = object : ArrayAdapter<BitmessageAddress>(
            activity,
            R.layout.subscription_row,
            R.id.name,
            LinkedList()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val result: View
                val v: ViewHolder
                if (convertView == null) {
                    val inflater = LayoutInflater.from(context)
                    val view = inflater.inflate(R.layout.subscription_row, parent, false)
                    v = ViewHolder(
                        ctx = context,
                        avatar = view.findViewById(R.id.avatar),
                        name = view.findViewById(R.id.name),
                        streamNumber = view.findViewById(R.id.stream_number),
                        subscribed = view.findViewById(R.id.subscribed)
                    )
                    view.tag = v
                    result = view
                } else {
                    v = convertView.tag as ViewHolder
                    result = convertView
                }
                getItem(position)?.let { item ->
                    v.avatar.setImageDrawable(Identicon(item))
                    v.name.text = item.toString()
                    v.streamNumber.text = v.ctx.getString(R.string.stream_number, item.stream)
                    v.subscribed.visibility = if (item.isSubscribed) View.VISIBLE else View.INVISIBLE
                }
                return result
            }
        }
        listAdapter = adapter
    }

    override fun onResume() {
        super.onResume()

        initFab(activity as MainActivity)
        updateList()
    }

    fun updateList() {
        adapter.clear()
        context?.let { context ->
            val addressRepo = Singleton.getAddressRepository(context)
            doAsync {
                addressRepo.getContactIds()
                    .map { addressRepo.getAddress(it) }
                    .forEach { address -> uiThread { adapter.add(address) } }

            }
        }
    }

    private fun initFab(activity: MainActivity) {
        activity.updateTitle(getString(R.string.contacts_and_subscriptions))
        val menu = FabSpeedDialMenu(activity)
        menu.add(R.string.scan_qr_code).setIcon(R.drawable.ic_action_qr_code)
        menu.add(R.string.create_contact).setIcon(R.drawable.ic_action_create_contact)
        FabUtils.initFab(activity, R.drawable.ic_action_add_contact, menu)
            .addOnMenuItemClickListener { _, _, itemId ->
                when (itemId) {
                    1 -> IntentIntegrator.forSupportFragment(this@AddressListFragment)
                        .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                        .initiateScan()
                    2 -> {
                        val intent = Intent(getActivity(), CreateAddressActivity::class.java)
                        startActivity(intent)
                    }
                    else -> {
                    }
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_address_list, container, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            val uri = Uri.parse(data.getStringExtra("SCAN_RESULT"))
            val intent = Intent(activity, CreateAddressActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }
    }

    override fun updateList(label: Void) = updateList()

    private data class ViewHolder(
        val ctx: Context,
        val avatar: ImageView,
        val name: TextView,
        val streamNumber: TextView,
        val subscribed: View
    )
}
