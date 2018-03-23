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
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import ch.dissem.apps.abit.adapter.ConversationAdapter
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Drawables
import ch.dissem.apps.abit.util.Strings.prepareMessageExtract
import ch.dissem.apps.abit.util.getDrawable
import ch.dissem.bitmessage.entity.Conversation
import ch.dissem.bitmessage.entity.Plaintext
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import kotlinx.android.synthetic.main.fragment_conversation_detail.*

/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a [MainActivity]
 * in two-pane mode (on tablets) or a [MessageDetailActivity]
 * on handsets.
 */
class ConversationDetailFragment : Fragment() {

    /**
     * The content this fragment is presenting.
     */
    private var item: Conversation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { arguments ->
            if (arguments.containsKey(ARG_ITEM)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = arguments.getSerializable(ARG_ITEM) as Conversation
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_conversation_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = activity ?: throw IllegalStateException("Fragment is not attached to an activity")

        // Show the dummy content as text in a TextView.
        item?.let { item ->
            subject.text = item.subject
            avatar.setImageDrawable(MultiIdenticon(item.participants))
            messages.adapter = ConversationAdapter(ctx, this@ConversationDetailFragment, item)
            messages.layoutManager = LinearLayoutManager(activity)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.conversation, menu)
        activity?.let { activity ->
            Drawables.addIcon(activity, menu, R.id.delete, GoogleMaterial.Icon.gmd_delete)
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
                R.id.delete -> {
                    item.messages.forEach {
                        Singleton.labeler.delete(it)
                        messageRepo.remove(it)
                    }
                    MainActivity.apply { updateUnread() }
                    activity?.onBackPressed()
                    return true
                }
                R.id.archive -> {
                    item.messages.forEach {
                        Singleton.labeler.archive(it)
                        messageRepo.save(it)

                    }
                    MainActivity.apply { updateUnread() }
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
            viewHolder.status.setImageResource(message.status.getDrawable())
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

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM = "item"
    }
}
