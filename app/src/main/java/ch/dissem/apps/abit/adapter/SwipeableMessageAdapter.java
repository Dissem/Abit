package ch.dissem.apps.abit.adapter;

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

/**
 * @author Christian Basler
 */
public class SwipeableMessageAdapter
    extends RecyclerView.Adapter<SwipeableMessageAdapter.MyViewHolder>
    implements SwipeableItemAdapter<SwipeableMessageAdapter.MyViewHolder>, SwipeableItemConstants {

    private List<Plaintext> data = Collections.emptyList();
    private EventListener eventListener;
    private View.OnClickListener itemViewOnClickListener;
    private View.OnClickListener swipeableViewContainerOnClickListener;

    private Label label;

    public interface EventListener {
        void onItemDeleted(Plaintext item);

        void onItemArchived(Plaintext item);

        void onItemViewClicked(View v, boolean pinned);
    }

    public static class MyViewHolder extends AbstractSwipeableItemViewHolder {
        public FrameLayout container;
        public final ImageView avatar;
        public final TextView sender;
        public final TextView subject;
        public final TextView extract;

        public MyViewHolder(View v) {
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
        itemViewOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemViewClick(v);
            }
        };
        swipeableViewContainerOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwipeableViewContainerClick(v);
            }
        };

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
            eventListener.onItemViewClicked(v, true); // pinned
        }
    }

    private void onSwipeableViewContainerClick(View v) {
        if (eventListener != null) {
            eventListener.onItemViewClicked(
                RecyclerViewAdapterUtils.getParentViewHolderItemView(v), false);  // not pinned
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
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.message_row, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final Plaintext item = data.get(position);

        // set listeners
        // (if the item is *pinned*, click event comes to the itemView)
        holder.itemView.setOnClickListener(itemViewOnClickListener);
        // (if the item is *not pinned*, click event comes to the container)
        holder.container.setOnClickListener(swipeableViewContainerOnClickListener);

        // set data
        holder.avatar.setImageDrawable(new Identicon(item.getFrom()));
        holder.sender.setText(item.getFrom().toString());
        holder.subject.setText(item.getSubject());
        holder.extract.setText(item.getText());
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
    public int onGetSwipeReactionType(MyViewHolder holder, int position, int x, int y) {
        if (label == null) {
            return REACTION_CAN_NOT_SWIPE_BOTH_H_WITH_RUBBER_BAND_EFFECT;
        }
        if (label.getType() == Label.Type.TRASH) {
            return REACTION_CAN_SWIPE_LEFT | REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT;
        }
        return REACTION_CAN_SWIPE_BOTH_H;
    }

    @Override
    public void onSetSwipeBackground(MyViewHolder holder, int position, int type) {
        int bgRes = 0;
        if (label == null) {
            bgRes = R.drawable.bg_swipe_item_neutral;
        } else {
            switch (type) {
                case DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
                    bgRes = R.drawable.bg_swipe_item_neutral;
                    break;
                case DRAWABLE_SWIPE_LEFT_BACKGROUND:
                    bgRes = R.drawable.bg_swipe_item_left;
                    break;
                case DRAWABLE_SWIPE_RIGHT_BACKGROUND:
                    if (label.getType() == Label.Type.TRASH) {
                        bgRes = R.drawable.bg_swipe_item_neutral;
                    } else {
                        bgRes = R.drawable.bg_swipe_item_right;
                    }
                    break;
            }
        }
        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override
    public SwipeResultAction onSwipeItem(MyViewHolder holder, final int position, int result) {
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
