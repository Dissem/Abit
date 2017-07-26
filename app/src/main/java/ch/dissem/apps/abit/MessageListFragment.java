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

package ch.dissem.apps.abit;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

import java.util.List;
import java.util.Stack;

import ch.dissem.apps.abit.adapter.SwipeableMessageAdapter;
import ch.dissem.apps.abit.listener.ListSelectionListener;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.FabUtils;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_BROADCAST;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_IDENTITY;
import static ch.dissem.apps.abit.MessageDetailFragment.isInTrash;

/**
 * A list fragment representing a list of Messages. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link MessageDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link ListSelectionListener}
 * interface.
 */
public class MessageListFragment extends Fragment implements ListHolder<Label> {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private SwipeableMessageAdapter adapter;
    private RecyclerView.Adapter wrappedAdapter;
    private RecyclerViewSwipeManager recyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager recyclerViewTouchActionGuardManager;

    private Label currentLabel;
    private MenuItem emptyTrashMenuItem;
    private AndroidMessageRepository messageRepo;
    private boolean activateOnItemClick;

    private Stack<Label> backStack = new Stack<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        messageRepo = Singleton.getMessageRepository(activity);

        if (backStack.isEmpty()) {
            doUpdateList(activity.getSelectedLabel());
        } else {
            doUpdateList(backStack.peek());
        }
    }

    @Override
    public void updateList(Label label) {
        if (currentLabel != null && !currentLabel.equals(label)) {
            backStack.push(currentLabel);
        }
        if (!isResumed()) {
            currentLabel = label;
            return;
        }

        doUpdateList(label);
    }

    private void doUpdateList(final Label label) {
        adapter.clear(label);
        if (label == null) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateTitle(getString(R.string.app_name));
            }
            adapter.notifyDataSetChanged();
            return;
        }
        currentLabel = label;
        if (emptyTrashMenuItem != null) {
            emptyTrashMenuItem.setVisible(label.getType() == Label.Type.TRASH);
        }
        if (getActivity() instanceof MainActivity) {
            MainActivity actionBarListener = (MainActivity) getActivity();
            if ("archive".equals(label.toString())) {
                actionBarListener.updateTitle(getString(R.string.archive));
            } else {
                actionBarListener.updateTitle(label.toString());
            }
        }

        new AsyncTask<Void, Plaintext, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Long> ids = messageRepo.findMessageIds(label);
                for (Long id : ids) {
                    Plaintext message = messageRepo.getMessage(id);
                    publishProgress(message);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Plaintext... values) {
                if (adapter != null) {
                    for (Plaintext message : values) {
                        adapter.add(message);
                    }
                }
            }
        }.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
        savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_list, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss
        // animation is running)
        recyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        recyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning
            (true);
        recyclerViewTouchActionGuardManager.setEnabled(true);

        // swipe manager
        recyclerViewSwipeManager = new RecyclerViewSwipeManager();

        //adapter
        adapter = new SwipeableMessageAdapter();
        adapter.setActivateOnItemClick(activateOnItemClick);
        adapter.setEventListener(new SwipeableMessageAdapter.EventListener() {
            @Override
            public void onItemDeleted(Plaintext item) {
                if (isInTrash(item)) {
                    messageRepo.remove(item);
                } else {
                    item.getLabels().clear();
                    item.addLabels(messageRepo.getLabels(Label.Type.TRASH));
                    messageRepo.save(item);
                }
            }

            @Override
            public void onItemArchived(Plaintext item) {
                item.getLabels().clear();
                messageRepo.save(item);
            }

            @Override
            public void onItemViewClicked(View v) {
                int position = recyclerView.getChildAdapterPosition(v);
                adapter.setSelectedPosition(position);
                if (position != RecyclerView.NO_POSITION) {
                    Plaintext item = adapter.getItem(position);
                    ((MainActivity) getActivity()).onItemSelected(item);
                }
            }
        });

        // wrap for swiping
        wrappedAdapter = recyclerViewSwipeManager.createWrappedAdapter(adapter);

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item
        // works properly.
        animator.setSupportsChangeAnimations(false);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(wrappedAdapter);  // requires *wrapped* adapter
        recyclerView.setItemAnimator(animator);

        recyclerView.addItemDecoration(new SimpleListDividerDecorator(
            ContextCompat.getDrawable(getContext(), R.drawable.list_divider_h), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of
        // touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        recyclerViewTouchActionGuardManager.attachRecyclerView(recyclerView);
        recyclerViewSwipeManager.attachRecyclerView(recyclerView);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        if (context instanceof MainActivity) {
            FabSpeedDialMenu menu = new FabSpeedDialMenu(context);
            menu.add(R.string.broadcast).setIcon(R.drawable.ic_action_broadcast);
            menu.add(R.string.personal_message).setIcon(R.drawable.ic_action_personal);
            FabUtils.initFab((MainActivity) context, R.drawable.ic_action_compose_message, menu)
                .addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
                    @Override
                    public void onMenuItemClick(FloatingActionButton floatingActionButton, @Nullable TextView textView, int itemId) {
                        BitmessageAddress identity = Singleton.getIdentity(getActivity());
                        if (identity == null) {
                            Toast.makeText(getActivity(), R.string.no_identity_warning,
                                Toast.LENGTH_LONG).show();
                        } else {
                            switch (itemId) {
                                case 1: {
                                    Intent intent = new Intent(getActivity(), ComposeMessageActivity.class);
                                    intent.putExtra(EXTRA_IDENTITY, identity);
                                    startActivity(intent);
                                    break;
                                }
                                case 2: {
                                    Intent intent = new Intent(getActivity(), ComposeMessageActivity.class);
                                    intent.putExtra(EXTRA_IDENTITY, identity);
                                    intent.putExtra(EXTRA_BROADCAST, true);
                                    startActivity(intent);
                                    break;
                                }
                                default:
                                    break;
                            }
                        }
                    }
                });
        }
        super.onAttach(context);
    }

    @Override
    public void onDestroyView() {
        if (recyclerViewSwipeManager != null) {
            recyclerViewSwipeManager.release();
            recyclerViewSwipeManager = null;
        }

        if (recyclerViewTouchActionGuardManager != null) {
            recyclerViewTouchActionGuardManager.release();
            recyclerViewTouchActionGuardManager = null;
        }

        if (recyclerView != null) {
            recyclerView.setItemAnimator(null);
            recyclerView.setAdapter(null);
            recyclerView = null;
        }

        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        adapter = null;
        layoutManager = null;

        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_list, menu);
        emptyTrashMenuItem = menu.findItem(R.id.empty_trash);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.empty_trash:
                if (currentLabel.getType() != Label.Type.TRASH) return true;

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        for (Plaintext message : messageRepo.findMessages(currentLabel)) {
                            messageRepo.remove(message);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        updateList(currentLabel);
                    }
                }.execute();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        if (adapter != null) {
            adapter.setActivateOnItemClick(activateOnItemClick);
        }
        this.activateOnItemClick = activateOnItemClick;
    }

    @Override
    public boolean showPreviousList() {
        if (backStack.isEmpty()) {
            return false;
        } else {
            doUpdateList(backStack.pop());
            return true;
        }
    }

    @Override
    public Label getCurrentLabel() {
        return currentLabel;
    }
}
