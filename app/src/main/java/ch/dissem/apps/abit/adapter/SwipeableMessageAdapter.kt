/*
 * Copyright 2015 Haruki Hasegawa
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

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import ch.dissem.apps.abit.Identicon
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.repository.AndroidLabelRepository.Companion.LABEL_ARCHIVE
import ch.dissem.apps.abit.util.Strings.prepareMessageExtract
import ch.dissem.apps.abit.util.getDrawable
import ch.dissem.apps.abit.util.getString
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants.*
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils
import java.util.*

/**
 * Adapted from the basic swipeable example by Haruki Hasegawa. See
 *
 * @author Christian Basler
 * @see [https://github.com/h6ah4i/android-advancedrecyclerview](https://github.com/h6ah4i/android-advancedrecyclerview)
 */
class SwipeableMessageAdapter : RecyclerView.Adapter<SwipeableMessageAdapter.ViewHolder>(),
    SwipeableItemAdapter<SwipeableMessageAdapter.ViewHolder>, SwipeableItemConstants {

    private val data = LinkedList<Plaintext>()
    var eventListener: EventListener? = null
    private val itemViewOnClickListener: View.OnClickListener
    private val swipeableViewContainerOnClickListener: View.OnClickListener

    private var label: Label? = null
    private var selectedPosition = -1
    private var activateOnItemClick: Boolean = false

    fun setActivateOnItemClick(activateOnItemClick: Boolean) {
        this.activateOnItemClick = activateOnItemClick
    }

    interface EventListener {
        fun onItemDeleted(item: Plaintext)

        fun onItemArchived(item: Plaintext)

        fun onItemViewClicked(v: View?)
    }

    class ViewHolder(v: View) : AbstractSwipeableItemViewHolder(v) {
        val container = v.findViewById<FrameLayout>(R.id.container)!!
        val avatar = v.findViewById<ImageView>(R.id.avatar)!!
        val status = v.findViewById<ImageView>(R.id.status)!!
        val sender = v.findViewById<TextView>(R.id.sender)!!
        val subject = v.findViewById<TextView>(R.id.subject)!!
        val extract = v.findViewById<TextView>(R.id.text)!!

        override fun getSwipeableContainerView() = container
    }

    init {
        itemViewOnClickListener = View.OnClickListener { view -> onItemViewClick(view) }
        swipeableViewContainerOnClickListener =
            View.OnClickListener { view -> onSwipeableViewContainerClick(view) }

        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    fun add(item: Plaintext) {
        data.add(item)
        notifyDataSetChanged()
    }

    fun addFirst(item: Plaintext) {
        val index = data.size
        data.addFirst(item)
        notifyItemInserted(index)
    }

    fun addAll(items: Collection<Plaintext>) {
        val index = data.size
        data.addAll(items)
        notifyItemRangeInserted(index, items.size)
    }

    fun remove(item: Plaintext) {
        val index = data.indexOf(item)
        data.removeAll { it.id == item.id }
        notifyItemRemoved(index)
    }

    fun update(item: Plaintext) {
        val index = data.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            data[index] = item
            notifyItemChanged(index)
        }
    }

    fun clear(newLabel: Label?) {
        label = newLabel
        data.clear()
        notifyDataSetChanged()
    }

    private fun onItemViewClick(v: View) {
        eventListener?.onItemViewClicked(v)
    }

    private fun onSwipeableViewContainerClick(v: View) {
        eventListener?.onItemViewClicked(
            RecyclerViewAdapterUtils.getParentViewHolderItemView(v)
        )
    }

    fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = data[position].id as Long

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.message_row, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.apply {
            if (activateOnItemClick) {
                container.setBackgroundResource(
                    if (position == selectedPosition)
                        R.drawable.bg_item_selected_state
                    else
                        R.drawable.bg_item_normal_state
                )
            }

            // set listeners
            // (if the item is *pinned*, click event comes to the itemView)
            itemView.setOnClickListener(itemViewOnClickListener)
            // (if the item is *not pinned*, click event comes to the container)
            container.setOnClickListener(swipeableViewContainerOnClickListener)

            // set data
            avatar.setImageDrawable(Identicon(item.from))
            status.setImageResource(item.status.getDrawable())
            status.contentDescription = holder.status.context.getString(item.status.getString())
            sender.text = item.from.toString()
            subject.text = prepareMessageExtract(item.subject)
            extract.text = prepareMessageExtract(item.text)
            if (item.isUnread()) {
                sender.typeface = Typeface.DEFAULT_BOLD
                subject.typeface = Typeface.DEFAULT_BOLD
            } else {
                sender.typeface = Typeface.DEFAULT
                subject.typeface = Typeface.DEFAULT
            }
        }
    }

    override fun getItemCount() = data.size

    override fun onGetSwipeReactionType(holder: ViewHolder, position: Int, x: Int, y: Int): Int =
        if (label === LABEL_ARCHIVE || label?.type == Label.Type.TRASH) {
            REACTION_CAN_SWIPE_LEFT or REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT
        } else {
            REACTION_CAN_SWIPE_BOTH_H
        }

    @SuppressLint("SwitchIntDef")
    override fun onSetSwipeBackground(holder: ViewHolder, position: Int, type: Int) =
        holder.itemView.setBackgroundResource(
            when (type) {
                DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> R.drawable.bg_swipe_item_neutral
                DRAWABLE_SWIPE_LEFT_BACKGROUND -> R.drawable.bg_swipe_item_left
                DRAWABLE_SWIPE_RIGHT_BACKGROUND -> if (label === LABEL_ARCHIVE || label?.type == Label.Type.TRASH) {
                    R.drawable.bg_swipe_item_neutral
                } else {
                    R.drawable.bg_swipe_item_right
                }
                else -> R.drawable.bg_swipe_item_neutral
            }
        )

    @SuppressLint("SwitchIntDef")
    override fun onSwipeItem(holder: ViewHolder, position: Int, result: Int) =
        when (result) {
            RESULT_SWIPED_RIGHT -> SwipeRightResultAction(this, position)
            RESULT_SWIPED_LEFT -> SwipeLeftResultAction(this, position)
            else -> null
        }

    override fun onSwipeItemStarted(holder: ViewHolder?, position: Int) = Unit

    fun setSelectedPosition(selectedPosition: Int) {
        val oldPosition = this.selectedPosition
        this.selectedPosition = selectedPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    private class SwipeLeftResultAction internal constructor(
        adapter: SwipeableMessageAdapter,
        position: Int
    ) : SwipeResultActionMoveToSwipedDirection() {
        private var adapter: SwipeableMessageAdapter? = adapter
        private val item = adapter.data[position]

        override fun onPerformAction() {
            adapter?.eventListener?.onItemDeleted(item)
        }

        override fun onCleanUp() {
            adapter = null
        }
    }

    private class SwipeRightResultAction internal constructor(
        adapter: SwipeableMessageAdapter,
        position: Int
    ) : SwipeResultActionRemoveItem() {
        private var adapter: SwipeableMessageAdapter? = adapter
        private val item = adapter.data[position]

        override fun onPerformAction() {
            adapter?.eventListener?.onItemArchived(item)
        }

        override fun onCleanUp() {
            adapter = null
        }
    }
}
