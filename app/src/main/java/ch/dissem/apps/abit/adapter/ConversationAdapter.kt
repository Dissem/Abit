package ch.dissem.apps.abit.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ch.dissem.apps.abit.*
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Constants
import ch.dissem.apps.abit.util.getDrawable
import ch.dissem.bitmessage.entity.Conversation
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.ports.MessageRepository


class ConversationAdapter internal constructor(
    ctx: Context,
    private val parent: Fragment,
    private val conversation: Conversation
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private val messageRepo = Singleton.getMessageRepository(ctx)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConversationAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val messageView = inflater.inflate(R.layout.item_message_detail, parent, false)

        // Return a new holder instance
        return ViewHolder(messageView, this.parent, messageRepo)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: ConversationAdapter.ViewHolder, position: Int) {
        // Get the data model based on position
        val message = conversation.messages[position]

        viewHolder.apply {
            item = message
            avatar.setImageDrawable(Identicon(message.from))
            sender.text = message.from.toString()
            val senderClickListener: (View) -> Unit = {
                MainActivity.apply {
                    onItemSelected(message.from)
                }
            }
            avatar.setOnClickListener(senderClickListener)
            sender.setOnClickListener(senderClickListener)

            recipient.text = message.to.toString()
            status.setImageResource(message.status.getDrawable())
            text.text = message.text

            Linkify.addLinks(text, Linkify.WEB_URLS)
            Linkify.addLinks(text,
                Constants.BITMESSAGE_ADDRESS_PATTERN,
                Constants.BITMESSAGE_URL_SCHEMA, null,
                Linkify.TransformFilter { match, _ -> match.group() }
            )

            labelAdapter.labels = message.labels.toList()

            // FIXME: I think that's not quite correct
            if (message.isUnread()) {
                Singleton.labeler.markAsRead(message)
                messageRepo.save(message)
                MainActivity.apply { updateUnread() }
            }

        }
    }

    override fun getItemCount() = conversation.messages.size

    class ViewHolder(
        itemView: View,
        parent: Fragment,
        messageRepo: MessageRepository
    ) : RecyclerView.ViewHolder(itemView) {
        var item: Plaintext? = null
        val avatar = itemView.findViewById<ImageView>(R.id.avatar)!!
        val sender = itemView.findViewById<TextView>(R.id.sender)!!
        val recipient = itemView.findViewById<TextView>(R.id.recipient)!!
        val status = itemView.findViewById<ImageView>(R.id.status)!!
        val menu = itemView.findViewById<ImageView>(R.id.menu)!!.also { view ->
            view.setOnClickListener {
                val popup = PopupMenu(itemView.context, view)
                popup.menuInflater.inflate(R.menu.message, popup.menu)
                popup.setOnMenuItemClickListener {
                    item?.let { item ->
                        when (it.itemId) {
                            R.id.reply -> {
                                ComposeMessageActivity.launchReplyTo(parent, item)
                                true
                            }
                            R.id.delete -> {
                                if (MessageDetailFragment.isInTrash(item)) {
                                    Singleton.labeler.delete(item)
                                    messageRepo.remove(item)
                                } else {
                                    Singleton.labeler.delete(item)
                                    messageRepo.save(item)
                                }
                                MainActivity.apply {
                                    updateUnread()
                                    onBackPressed()
                                }
                                true
                            }
                            R.id.mark_unread -> {
                                Singleton.labeler.markAsUnread(item)
                                messageRepo.save(item)
                                MainActivity.apply { updateUnread() }
                                true
                            }
                            R.id.archive -> {
                                Singleton.labeler.archive(item)
                                messageRepo.save(item)
                                MainActivity.apply { updateUnread() }
                                true
                            }
                            else -> false
                        }
                    } ?: false
                }
                popup.show()
            }
        }
        val text = itemView.findViewById<TextView>(R.id.text)!!.apply {
            linksClickable = true
            setTextIsSelectable(true)
        }
        val labelAdapter = LabelAdapter(itemView.context, emptySet<Label>())
        val labels = itemView.findViewById<RecyclerView>(R.id.labels)!!.apply {
            adapter = labelAdapter
            layoutManager = GridLayoutManager(itemView.context, 2)
        }
    }
}
