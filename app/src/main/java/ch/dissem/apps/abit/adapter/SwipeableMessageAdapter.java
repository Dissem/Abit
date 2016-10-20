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

package ch.dissem.apps.abit.adapter;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action
    .SwipeResultActionMoveToSwipedDirection;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils;

import java.util.Collections;
import java.util.List;

import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import static ch.dissem.apps.abit.util.Strings.normalizeWhitespaces;

/**
 * Adapted from the basic swipeable example by Haruki Hasegawa. See
 *
 * @author Christian Basler
 * @see <a href="https://github.com/h6ah4i/android-advancedrecyclerview">
 * https://github.com/h6ah4i/android-advancedrecyclerview</a>
 */
public class SwipeableMessageAdapter
    extends RecyclerView.Adapter<SwipeableMessageAdapter.ViewHolder>
    implements SwipeableItemAdapter<SwipeableMessageAdapter.ViewHolder>, SwipeableItemConstants {

    private List<Plaintext> data = Collections.emptyList();
    private EventListener eventListener;
    private final View.OnClickListener itemViewOnClickListener;
    private final View.OnClickListener swipeableViewContainerOnClickListener;

    private Label label;
    private int selectedPosition;
    private boolean activateOnItemClick;

    public void setActivateOnItemClick(boolean activateOnItemClick) {
        this.activateOnItemClick = activateOnItemClick;
    }

    public interface EventListener {
        void onItemDeleted(Plaintext item);

        void onItemArchived(Plaintext item);

        void onItemViewClicked(View v);
    }

    @SuppressWarnings("WeakerAccess")
    static class ViewHolder extends AbstractSwipeableItemViewHolder {
        public final FrameLayout container;
        public final ImageView avatar;
        public final TextView sender;
        public final TextView subject;
        public final TextView extract;

        ViewHolder(View v) {
            super(v);
            container = (FrameLayout) v.findViewById(R.id.container);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            sender = (TextView) v.findViewById(R.id.sender);
            subject = (TextView) v.findViewById(R.id.subject);
            extract = (TextView) v.findViewById(R.id.text);
        }

        @Override
        public View getSwipeableContainerView() {
            return container;
        }
    }

    public SwipeableMessageAdapter() {
        itemViewOnClickListener = this::onItemViewClick;
        swipeableViewContainerOnClickListener = this::onSwipeableViewContainerClick;

        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    public void setData(Label label, List<Plaintext> data) {
        this.label = label;
        this.data = data;
    }

    private void onItemViewClick(View v) {
        if (eventListener != null) {
            eventListener.onItemViewClicked(v);
        }
    }

    private void onSwipeableViewContainerClick(View v) {
        if (eventListener != null) {
            eventListener.onItemViewClicked(
                RecyclerViewAdapterUtils.getParentViewHolderItemView(v));
        }
    }

    public Plaintext getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) data.get(position).getId();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.message_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Plaintext item = data.get(position);

        if (activateOnItemClick) {
            holder.container.setBackgroundResource(
                position == selectedPosition
                    ? R.drawable.bg_item_selected_state
                    : R.drawable.bg_item_normal_state
            );
        }

        // set listeners
        // (if the item is *pinned*, click event comes to the itemView)
        holder.itemView.setOnClickListener(itemViewOnClickListener);
        // (if the item is *not pinned*, click event comes to the container)
        holder.container.setOnClickListener(swipeableViewContainerOnClickListener);

        // set data
        holder.avatar.setImageDrawable(new Identicon(item.getFrom()));
        holder.sender.setText(item.getFrom().toString());
        holder.subject.setText(normalizeWhitespaces(item.getSubject()));
        holder.extract.setText(normalizeWhitespaces(item.getText()));
        if (item.isUnread()) {
            holder.sender.setTypeface(Typeface.DEFAULT_BOLD);
            holder.subject.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            holder.sender.setTypeface(Typeface.DEFAULT);
            holder.subject.setTypeface(Typeface.DEFAULT);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int onGetSwipeReactionType(ViewHolder holder, int position, int x, int y) {
        if (label == null || label.getType() == Label.Type.TRASH) {
            return REACTION_CAN_SWIPE_LEFT | REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT;
        }
        return REACTION_CAN_SWIPE_BOTH_H;
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onSetSwipeBackground(ViewHolder holder, int position, int type) {
        int bgRes = 0;
        switch (type) {
            case DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_neutral;
                break;
            case DRAWABLE_SWIPE_LEFT_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_left;
                break;
            case DRAWABLE_SWIPE_RIGHT_BACKGROUND:
                if (label == null || label.getType() == Label.Type.TRASH) {
                    bgRes = R.drawable.bg_swipe_item_neutral;
                } else {
                    bgRes = R.drawable.bg_swipe_item_right;
                }
                break;
        }
        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public SwipeResultAction onSwipeItem(ViewHolder holder, final int position, int result) {
        switch (result) {
            // swipe right
            case RESULT_SWIPED_RIGHT:
                return new SwipeRightResultAction(this, position);
            case RESULT_SWIPED_LEFT:
                return new SwipeLeftResultAction(this, position);
            // other --- do nothing
            case RESULT_CANCELED:
            default:
                return null;
        }
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setSelectedPosition(int selectedPosition) {
        int oldPosition = this.selectedPosition;
        this.selectedPosition = selectedPosition;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    private static class SwipeLeftResultAction extends SwipeResultActionMoveToSwipedDirection {
        private SwipeableMessageAdapter adapter;
        private final int position;
        private final Plaintext item;

        SwipeLeftResultAction(SwipeableMessageAdapter adapter, int position) {
            this.adapter = adapter;
            this.position = position;
            this.item = adapter.data.get(position);
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            adapter.data.remove(position);
            adapter.notifyItemRemoved(position);
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();

            if (adapter.eventListener != null) {
                adapter.eventListener.onItemDeleted(item);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            adapter = null;
        }
    }

    private static class SwipeRightResultAction extends SwipeResultActionRemoveItem {
        private SwipeableMessageAdapter adapter;
        private final int position;
        private final Plaintext item;

        SwipeRightResultAction(SwipeableMessageAdapter adapter, int position) {
            this.adapter = adapter;
            this.position = position;
            this.item = adapter.data.get(position);
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            adapter.data.remove(position);
            adapter.notifyItemRemoved(position);
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();

            if (adapter.eventListener != null) {
                adapter.eventListener.onItemArchived(item);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            adapter = null;
        }
    }
}
