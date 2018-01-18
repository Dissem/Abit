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

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Drawables
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.wif.WifExporter
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import kotlinx.android.synthetic.main.fragment_address_detail.*


/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a [MainActivity]
 * in two-pane mode (on tablets) or a [MessageDetailActivity]
 * on handsets.
 */
class AddressDetailFragment : Fragment() {

    /**
     * The content this fragment is presenting.
     */
    private var item: BitmessageAddress? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { arguments ->
            if (arguments.containsKey(ARG_ITEM)) {
                item = arguments.getSerializable(ARG_ITEM) as BitmessageAddress
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.address, menu)

        val ctx = activity!!
        Drawables.addIcon(ctx, menu, R.id.write_message, GoogleMaterial.Icon.gmd_mail)
        Drawables.addIcon(ctx, menu, R.id.share, GoogleMaterial.Icon.gmd_share)
        Drawables.addIcon(ctx, menu, R.id.delete, GoogleMaterial.Icon.gmd_delete)
        Drawables.addIcon(ctx, menu, R.id.export, CommunityMaterial.Icon.cmd_export).isVisible = item?.privateKey != null

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        val item = item ?: return false
        val ctx = activity ?: return false
        when (menuItem.itemId) {
            R.id.write_message -> {
                val identity = Singleton.getIdentity(ctx)
                if (identity == null) {
                    Toast.makeText(ctx, R.string.no_identity_warning, Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(ctx, ComposeMessageActivity::class.java)
                    intent.putExtra(ComposeMessageActivity.EXTRA_IDENTITY, identity)
                    intent.putExtra(ComposeMessageActivity.EXTRA_RECIPIENT, item)
                    startActivity(intent)
                }
                return true
            }
            R.id.delete -> {
                val warning = if (item.privateKey != null)
                    R.string.delete_identity_warning
                else
                    R.string.delete_contact_warning
                AlertDialog.Builder(ctx)
                    .setMessage(warning)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        Singleton.getAddressRepository(ctx).remove(item)
                        MainActivity.apply {
                            if (item.privateKey != null) {
                                removeIdentityEntry(item)
                            }
                        }
                        this.item = null
                        ctx.onBackPressed()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                return true
            }
            R.id.export -> {
                AlertDialog.Builder(ctx)
                    .setMessage(R.string.confirm_export)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TITLE,
                                "$item$EXPORT_POSTFIX"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                WifExporter(Singleton.getBitmessageContext(ctx)).apply {
                                    addIdentity(item)
                                }.toString()
                            )
                        }
                        startActivity(Intent.createChooser(shareIntent, null))
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                return true
            }
            R.id.share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, item.address)
                startActivity(Intent.createChooser(shareIntent, null))
                return true
            }
            else -> return false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        = inflater.inflate(R.layout.fragment_address_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show the dummy content as text in a TextView.
        item?.let { item ->
            activity?.let { activity ->
                when {
                    item.isChan -> activity.setTitle(R.string.title_chan_detail)
                    item.privateKey != null -> activity.setTitle(R.string.title_identity_detail)
                    item.isSubscribed -> activity.setTitle(R.string.title_subscription_detail)
                    else -> activity.setTitle(R.string.title_contact_detail)
                }
            }

            avatar.setImageDrawable(Identicon(item))
            name.setText(item.toString())
            name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit // Nothing to do

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit // Nothing to do

                override fun afterTextChanged(s: Editable) {
                    item.alias = s.toString()
                }
            })
            address.text = item.address
            address.isSelected = true
            stream_number.text = getString(R.string.stream_number, item.stream)
            if (item.privateKey == null) {
                active.isChecked = item.isSubscribed
                active.setOnCheckedChangeListener { _, checked -> item.isSubscribed = checked }

                if (item.pubkey == null) {
                    pubkey_available.alpha = 0.3f
                    pubkey_available_desc.setText(R.string.pubkey_not_available)
                }
            } else {
                active.visibility = View.GONE
                pubkey_available.visibility = View.GONE
                pubkey_available_desc.visibility = View.GONE
            }

            // QR code
            qr_code.setImageBitmap(Drawables.qrCode(item))
        }
    }

    override fun onPause() {
        item?.let { item ->
            Singleton.getAddressRepository(context!!).save(item)
            if (item.privateKey != null) {
                MainActivity.apply { updateIdentityEntry(item) }
            }
        }
        super.onPause()
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        val ARG_ITEM = "item"
        val EXPORT_POSTFIX = ".keys.dat"
    }
}
