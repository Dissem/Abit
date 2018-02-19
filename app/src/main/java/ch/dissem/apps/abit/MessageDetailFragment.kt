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

package ch.dissem.apps.abit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.util.Linkify
import android.text.util.Linkify.WEB_URLS
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Assets
import ch.dissem.apps.abit.util.Constants.BITMESSAGE_ADDRESS_PATTERN
import ch.dissem.apps.abit.util.Constants.BITMESSAGE_URL_SCHEMA
import ch.dissem.apps.abit.util.Drawables
import ch.dissem.apps.abit.util.Labels
import ch.dissem.apps.abit.util.Strings.prepareMessageExtract
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.view.IconicsImageView
import kotlinx.android.synthetic.main.fragment_message_detail.*
import java.util.*

/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a [MainActivity]
 * in two-pane mode (on tablets) or a [MessageDetailActivity]
 * on handsets.
 */
class MessageDetailFragment : Fragment() {

    /**
     * The content this fragment is presenting.
     */
    private var item: Plaintext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { arguments ->
            if (arguments.containsKey(ARG_ITEM)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = arguments.getSerializable(ARG_ITEM) as Plaintext
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_message_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = activity ?: throw IllegalStateException("Fragment is not attached to an activity")

        // Show the dummy content as text in a TextView.
        item?.let { item ->
            subject.text = item.subject
            status.setImageResource(Assets.getStatusDrawable(item.status))
            status.contentDescription = getString(Assets.getStatusString(item.status))
            avatar.setImageDrawable(Identicon(item.from))
            val senderClickListener: (View) -> Unit = {
                MainActivity.apply {
                    onItemSelected(item.from)
                }
            }
            avatar.setOnClickListener(senderClickListener)
            sender.setOnClickListener(senderClickListener)
            sender.text = item.from.toString()
            item.to?.let { to ->
                recipient.text = to.toString()
            } ?: {
                if (item.type == Plaintext.Type.BROADCAST) {
                    recipient.setText(R.string.broadcast)
                }
            }.invoke()
            val labelAdapter = LabelAdapter(ctx, item.labels)
            labels.adapter = labelAdapter
            labels.layoutManager = GridLayoutManager(activity, 2)

            text.text = item.text

            Linkify.addLinks(text, WEB_URLS)
            Linkify.addLinks(text, BITMESSAGE_ADDRESS_PATTERN, BITMESSAGE_URL_SCHEMA, null,
                Linkify.TransformFilter { match, _ -> match.group() }
            )

            text.linksClickable = true
            text.setTextIsSelectable(true)

            val messageRepo = Singleton.getMessageRepository(ctx)
            if (item.isUnread()) {
                Singleton.labeler.markAsRead(item)
                (activity as? MainActivity)?.updateUnread()
                messageRepo.save(item)
            }
            val parents = ArrayList<Plaintext>(item.parents.size)
            for (parentIV in item.parents) {
                val parent = messageRepo.getMessage(parentIV)
                if (parent != null) {
                    parents.add(parent)
                }
            }
            showRelatedMessages(ctx, view, R.id.parents, parents)
            showRelatedMessages(ctx, view, R.id.responses, messageRepo.findResponses(item))
        }
    }

    private fun showRelatedMessages(
        ctx: Context,
        rootView: View, @IdRes id: Int,
        messages: List<Plaintext>
    ) {
        val recyclerView = rootView.findViewById<RecyclerView>(id)
        val adapter = RelatedMessageAdapter(ctx, messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.message, menu)
        activity?.let { activity ->
            Drawables.addIcon(activity, menu, R.id.reply, GoogleMaterial.Icon.gmd_reply)
            Drawables.addIcon(activity, menu, R.id.delete, GoogleMaterial.Icon.gmd_delete)
            Drawables.addIcon(
                activity, menu, R.id.mark_unread, GoogleMaterial.Icon
                    .gmd_markunread
            )
            Drawables.addIcon(activity, menu, R.id.archive, GoogleMaterial.Icon.gmd_archive)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        val messageRepo = Singleton.getMessageRepository(
            context ?: throw IllegalStateException("No context available")
        )
        item?.let { item ->
            when (menuItem.itemId) {
                R.id.reply -> {
                    ComposeMessageActivity.launchReplyTo(this, item)
                    return true
                }
                R.id.delete -> {
                    if (isInTrash(item)) {
                        Singleton.labeler.delete(item)
                        messageRepo.remove(item)
                    } else {
                        Singleton.labeler.delete(item)
                        messageRepo.save(item)
                    }
                    (activity as? MainActivity)?.updateUnread()
                    activity?.onBackPressed()
                    return true
                }
                R.id.mark_unread -> {
                    Singleton.labeler.markAsUnread(item)
                    messageRepo.save(item)
                    (activity as? MainActivity)?.updateUnread()
                    return true
                }
                R.id.archive -> {
                    if (item.isUnread() && activity is MainActivity) {
                        (activity as MainActivity).updateUnread()
                    }
                    Singleton.labeler.archive(item)
                    messageRepo.save(item)
                    (activity as? MainActivity)?.updateUnread()
                    return true
                }
                else -> return false
            }
        }
        return false
    }

    private class RelatedMessageAdapter internal constructor(
        private val ctx: Context,
        private val messages: List<Plaintext>
    ) : RecyclerView.Adapter<RelatedMessageAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RelatedMessageAdapter.ViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)

            // Inflate the custom layout
            val contactView = inflater.inflate(R.layout.item_message_minimized, parent, false)

            // Return a new holder instance
            return ViewHolder(contactView)
        }

        // Involves populating data into the item through holder
        override fun onBindViewHolder(viewHolder: RelatedMessageAdapter.ViewHolder, position: Int) {
            // Get the data model based on position
            val message = messages[position]

            viewHolder.avatar.setImageDrawable(Identicon(message.from))
            viewHolder.status.setImageResource(Assets.getStatusDrawable(message.status))
            viewHolder.sender.text = message.from.toString()
            viewHolder.extract.text = prepareMessageExtract(message.text)
            viewHolder.item = message
        }

        // Returns the total count of items in the list
        override fun getItemCount() = messages.size

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal val avatar = itemView.findViewById<ImageView>(R.id.avatar)
            internal val status = itemView.findViewById<ImageView>(R.id.status)
            internal val sender = itemView.findViewById<TextView>(R.id.sender)
            internal val extract = itemView.findViewById<TextView>(R.id.text)
            internal var item: Plaintext? = null

            init {
                itemView.setOnClickListener {
                    if (ctx is MainActivity) {
                        item?.let { ctx.onItemSelected(it) }
                    } else {
                        val detailIntent = Intent(ctx, MessageDetailActivity::class.java)
                        detailIntent.putExtra(MessageDetailFragment.ARG_ITEM, item)
                        ctx.startActivity(detailIntent)
                    }
                }
            }
        }
    }

    private class LabelAdapter internal constructor(private val ctx: Context, labels: Set<Label>) :
        RecyclerView.Adapter<LabelAdapter.ViewHolder>() {

        private val labels = labels.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelAdapter.ViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)

            // Inflate the custom layout
            val contactView = inflater.inflate(R.layout.item_label, parent, false)

            // Return a new holder instance
            return ViewHolder(contactView)
        }

        // Involves populating data into the item through holder
        override fun onBindViewHolder(viewHolder: LabelAdapter.ViewHolder, position: Int) {
            // Get the data model based on position
            val label = labels[position]

            viewHolder.icon.icon?.color(Labels.getColor(label))
            viewHolder.icon.icon?.icon(Labels.getIcon(label))
            viewHolder.label.text = Labels.getText(label, ctx)
        }

        override fun getItemCount() = labels.size

        internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var icon = itemView.findViewById<IconicsImageView>(R.id.icon)!!
            var label = itemView.findViewById<TextView>(R.id.label)!!
        }
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        val ARG_ITEM = "item"

        fun isInTrash(item: Plaintext?) = item?.labels?.any { it.type == Label.Type.TRASH } == true
    }
}
