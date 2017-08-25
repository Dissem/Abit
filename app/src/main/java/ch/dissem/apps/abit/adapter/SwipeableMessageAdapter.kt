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

import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils

import java.util.LinkedList

import ch.dissem.apps.abit.Identicon
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.util.Assets
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label

import ch.dissem.apps.abit.repository.AndroidMessageRepository.Companion.LABEL_ARCHIVE
import ch.dissem.apps.abit.util.Strings.prepareMessageExtract

/**
 * Adapted from the basic swipeable example by Haruki Hasegawa. See
 *
 * @author Christian Basler
 * @see [
 * https://github.com/h6ah4i/android-advancedrecyclerview](https://github.com/h6ah4i/android-advancedrecyclerview)
 */
class SwipeableMessageAdapter : RecyclerView.Adapter<SwipeableMessageAdapter.ViewHolder>(), SwipeableItemAdapter<SwipeableMessageAdapter.ViewHolder>, SwipeableItemConstants {

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
        val container = v.findViewById(R.id.container) as FrameLayout
        val avatar = v.findViewById(R.id.avatar) as ImageView
        val status = v.findViewById(R.id.status) as ImageView
        val sender = v.findViewById(R.id.sender) as TextView
        val subject = v.findViewById(R.id.subject) as TextView
        val extract = v.findViewById(R.id.text) as TextView

        override fun getSwipeableContainerView() = container
    }

    init {
        itemViewOnClickListener = View.OnClickListener { view -> onItemViewClick(view) }
        swipeableViewContainerOnClickListener = View.OnClickListener { view -> onSwipeableViewContainerClick(view) }

        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    fun add(item: Plaintext) {
        data.add(item)
        notifyDataSetChanged()
    }

    fun clear(newLabel: Label?) {
        label = newLabel
        data.clear()
        notifyDataSetChanged()
    }

    private fun onItemViewClick(v: View) {
        if (eventListener != null) {
            eventListener!!.onItemViewClicked(v)
        }
    }

    private fun onSwipeableViewContainerClick(v: View) {
        if (eventListener != null) {
            eventListener!!.onItemViewClicked(
                    RecyclerViewAdapterUtils.getParentViewHolderItemView(v))
        }
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

        if (activateOnItemClick) {
            holder.container.setBackgroundResource(
                    if (position == selectedPosition)
                        R.drawable.bg_item_selected_state
                    else
                        R.drawable.bg_item_normal_state
            )
        }

        // set listeners
        // (if the item is *pinned*, click event comes to the itemView)
        holder.itemView.setOnClickListener(itemViewOnClickListener)
        // (if the item is *not pinned*, click event comes to the container)
        holder.container.setOnClickListener(swipeableViewContainerOnClickListener)

        // set data
        holder.avatar.setImageDrawable(Identicon(item.from))
        holder.status.setImageResource(Assets.getStatusDrawable(item.status))
        holder.status.contentDescription = holder.status.context.getString(Assets.getStatusString(item.status))
        holder.sender.text = item.from.toString()
        holder.subject.text = prepareMessageExtract(item.subject)
        holder.extract.text = prepareMessageExtract(item.text)
        if (item.isUnread()) {
            holder.sender.typeface = Typeface.DEFAULT_BOLD
            holder.subject.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.sender.typeface = Typeface.DEFAULT
            holder.subject.typeface = Typeface.DEFAULT
        }
    }

    override fun getItemCount() = data.size

    override fun onGetSwipeReactionType(holder: ViewHolder, position: Int, x: Int, y: Int): Int {
        return if (label === LABEL_ARCHIVE || label!!.type == Label.Type.TRASH) {
            SwipeableItemConstants.REACTION_CAN_SWIPE_LEFT or SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT
        } else SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    @SuppressLint("SwitchIntDef")
    override fun onSetSwipeBackground(holder: ViewHolder, position: Int, type: Int) {
        var bgRes = 0
        when (type) {
            SwipeableItemConstants.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_neutral
            SwipeableItemConstants.DRAWABLE_SWIPE_LEFT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_left
            SwipeableItemConstants.DRAWABLE_SWIPE_RIGHT_BACKGROUND -> if (label === LABEL_ARCHIVE || label!!.type == Label.Type.TRASH) {
                bgRes = R.drawable.bg_swipe_item_neutral
            } else {
                bgRes = R.drawable.bg_swipe_item_right
            }
        }
        holder.itemView.setBackgroundResource(bgRes)
    }

    @SuppressLint("SwitchIntDef")
    override fun onSwipeItem(holder: ViewHolder, position: Int, result: Int): SwipeResultAction? {
        when (result) {
            SwipeableItemConstants.RESULT_SWIPED_RIGHT -> return SwipeRightResultAction(this, position)
            SwipeableItemConstants.RESULT_SWIPED_LEFT -> return SwipeLeftResultAction(this, position)
            else -> return null
        }
    }

    fun setSelectedPosition(selectedPosition: Int) {
        val oldPosition = this.selectedPosition
        this.selectedPosition = selectedPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    private class SwipeLeftResultAction internal constructor(adapter: SwipeableMessageAdapter, private val position: Int) : SwipeResultActionMoveToSwipedDirection() {
        private var adapter: SwipeableMessageAdapter? = adapter
        private val item = adapter.data[position]

        override fun onPerformAction() {
            super.onPerformAction()

            adapter?.data?.removeAt(position)
            adapter?.notifyItemRemoved(position)
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
            adapter?.eventListener?.onItemDeleted(item)
        }

        override fun onCleanUp() {
            super.onCleanUp()
            // clear the references
            adapter = null
        }
    }

    private class SwipeRightResultAction internal constructor(adapter: SwipeableMessageAdapter, private val position: Int) : SwipeResultActionRemoveItem() {
        private var adapter: SwipeableMessageAdapter? = adapter
        private val item = adapter.data[position]

        override fun onPerformAction() {
            super.onPerformAction()

            adapter?.data?.removeAt(position)
            adapter?.notifyItemRemoved(position)
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
            adapter?.eventListener?.onItemArchived(item)
        }

        override fun onCleanUp() {
            super.onCleanUp()
            // clear the references
            adapter = null
        }
    }
}
