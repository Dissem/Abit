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


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.view.*
import android.widget.Toast
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_BROADCAST
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_IDENTITY
import ch.dissem.apps.abit.adapter.SwipeableConversationAdapter
import ch.dissem.apps.abit.listener.ListSelectionListener
import ch.dissem.apps.abit.repository.AndroidMessageRepository
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.service.Singleton.currentLabel
import ch.dissem.apps.abit.util.preferences
import ch.dissem.bitmessage.entity.Conversation
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.utils.ConversationService
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.android.synthetic.main.fragment_message_list.*
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.uiThread
import java.util.*

private const val PAGE_SIZE = 15

/**
 * A list fragment representing a list of Messages. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a [MessageDetailFragment].
 *
 *
 * Activities containing this fragment MUST implement the [ListSelectionListener]
 * interface.
 */
class ConversationListFragment : Fragment(), ListHolder<Label> {

    private var isLoading = false
    private var isLastPage = false

    private var layoutManager: LinearLayoutManager? = null
    private var swipeableConversationAdapter: SwipeableConversationAdapter? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private var recyclerViewTouchActionGuardManager: RecyclerViewTouchActionGuardManager? = null

    private val recyclerViewOnScrollListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            layoutManager?.let { layoutManager ->
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreItems()
                    }
                }
            }
        }
    }

    private var emptyTrashMenuItem: MenuItem? = null
    private var deleteAllMenuItem: MenuItem? = null
    private lateinit var messageRepo: AndroidMessageRepository
    private lateinit var conversationService: ConversationService
    private var activateOnItemClick: Boolean = false

    private val backStack = Stack<Label>()

    fun loadMoreItems() {
        isLoading = true
        swipeableConversationAdapter?.let { messageAdapter ->
            doAsync {
                val conversationIds = messageRepo.findConversations(
                    currentLabel.value,
                    messageAdapter.itemCount,
                    PAGE_SIZE,
                    context?.preferences?.separateIdentities == true
                )
                conversationIds.forEach { conversationId ->
                    val conversation = conversationService.getConversation(conversationId)
                    onUiThread {
                        messageAdapter.add(conversation)
                    }
                }
                isLoading = false
                isLastPage = conversationIds.size < PAGE_SIZE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        val activity = activity as MainActivity
        initFab(activity)
        messageRepo = Singleton.getMessageRepository(activity)
        conversationService = Singleton.getConversationService(activity)

        currentLabel.addObserver(this) { new -> doUpdateList(new) }
        doUpdateList(currentLabel.value)
    }

    override fun onPause() {
        currentLabel.removeObserver(this)
        super.onPause()
    }

    override fun reloadList() = doUpdateList(currentLabel.value)

    private fun doUpdateList(label: Label?) {
        val mainActivity = activity as? MainActivity
        swipeableConversationAdapter?.clear(label)
        if (label == null) {
            mainActivity?.updateTitle(getString(R.string.app_name))
            swipeableConversationAdapter?.notifyDataSetChanged()
            return
        }
        emptyTrashMenuItem?.isVisible = label.type == Label.Type.TRASH
        // I'm not yet sure if it's a good idea in conversation views, so it's off for now
        deleteAllMenuItem?.isVisible = false

        mainActivity?.apply {
            if ("archive" == label.toString()) {
                updateTitle(getString(R.string.archive))
            } else {
                updateTitle(label.toString())
            }
        }

        loadMoreItems()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_message_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = context ?: throw IllegalStateException("No context available")

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss
        // animation is running)
        val touchActionGuardManager = RecyclerViewTouchActionGuardManager().apply {
            setInterceptVerticalScrollingWhileAnimationRunning(true)
            isEnabled = true
        }

        // swipe manager
        val swipeManager = RecyclerViewSwipeManager()

        //swipeableConversationAdapter
        val adapter = SwipeableConversationAdapter(context).apply {
            setActivateOnItemClick(activateOnItemClick)
        }
        adapter.eventListener = object : SwipeableConversationAdapter.EventListener {
            override fun onItemDeleted(item: Conversation) {
                item.messages.forEach {
                    Singleton.labeler.delete(it)
                    messageRepo.save(it)
                }
            }

            override fun onItemArchived(item: Conversation) {
                item.messages.forEach { Singleton.labeler.archive(it) }
            }

            override fun onItemViewClicked(v: View?) {
                val position = recycler_view.getChildAdapterPosition(v)
                adapter.setSelectedPosition(position)
                if (position != RecyclerView.NO_POSITION) {
                    MainActivity.apply { onItemSelected(adapter.getItem(position)) }
                }
            }
        }

        // wrap for swiping
        wrappedAdapter = swipeManager.createWrappedAdapter(adapter)

        val animator = SwipeDismissItemAnimator()

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item
        // works properly.
        animator.supportsChangeAnimations = false

        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = wrappedAdapter  // requires *wrapped* swipeableConversationAdapter
        recycler_view.itemAnimator = animator
        recycler_view.addOnScrollListener(recyclerViewOnScrollListener)

        recycler_view.addItemDecoration(
            SimpleListDividerDecorator(
                ContextCompat.getDrawable(context, R.drawable.list_divider_h), true
            )
        )

        // NOTE:
        // The initialization order is very important! This order determines the priority of
        // touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        touchActionGuardManager.attachRecyclerView(recycler_view)
        swipeManager.attachRecyclerView(recycler_view)

        recyclerViewTouchActionGuardManager = touchActionGuardManager
        recyclerViewSwipeManager = swipeManager
        swipeableConversationAdapter = adapter

//   FIXME     Singleton.updateMessageListAdapterInListener(adapter)
    }

    private fun initFab(context: MainActivity) {
        val menu = FabSpeedDialMenu(context)
        menu.add(R.string.broadcast).setIcon(R.drawable.ic_action_broadcast)
        menu.add(R.string.personal_message).setIcon(R.drawable.ic_action_personal)
        context.initFab(R.drawable.ic_action_compose_message, menu)
            .addOnMenuItemClickListener { _, _, itemId ->
                val identity = Singleton.getIdentity(context)
                if (identity == null) {
                    Toast.makeText(
                        activity, R.string.no_identity_warning,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    when (itemId) {
                        1 -> {
                            val intent = Intent(activity, ComposeMessageActivity::class.java)
                            intent.putExtra(EXTRA_IDENTITY, identity)
                            intent.putExtra(EXTRA_BROADCAST, true)
                            startActivity(intent)
                        }
                        2 -> {
                            val intent = Intent(activity, ComposeMessageActivity::class.java)
                            intent.putExtra(EXTRA_IDENTITY, identity)
                            startActivity(intent)
                        }
                        else -> {
                        }
                    }
                }
            }
    }

    override fun onDestroyView() {
        recyclerViewSwipeManager?.release()
        recyclerViewSwipeManager = null

        recyclerViewTouchActionGuardManager?.release()
        recyclerViewTouchActionGuardManager = null

        recycler_view.itemAnimator = null
        recycler_view.adapter = null

        wrappedAdapter?.let { WrapperAdapterUtils.releaseAll(it) }
        wrappedAdapter = null

        swipeableConversationAdapter = null
        layoutManager = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.message_list, menu)
        emptyTrashMenuItem = menu.findItem(R.id.empty_trash)
        deleteAllMenuItem = menu.findItem(R.id.delete_all)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.empty_trash -> {
                currentLabel.value?.let { label ->
                    if (label.type != Label.Type.TRASH) return true

                    deleteAllMessages(label)
                }
                return true
            }
            R.id.delete_all -> {
                currentLabel.value?.let { label ->
                    alert(
                        title = R.string.delete_all_messages_in_list,
                        message = R.string.delete_all_messages_in_list_ask
                    ) {
                        positiveButton(R.string.delete) {
                            deleteAllMessages(label)
                        }
                        cancelButton { }
                    }.show()
                }
                return true
            }
            else -> return false
        }
    }

    private fun deleteAllMessages(label: Label) {
        doAsync {
            for (message in messageRepo.findMessages(label, 0, 0, context?.preferences?.separateIdentities == true)) {
                messageRepo.remove(message)
            }

            uiThread { doUpdateList(label) }
        }
    }

    override fun updateList(label: Label) {
        currentLabel.value = label
    }

    override fun setActivateOnItemClick(activateOnItemClick: Boolean) {
        swipeableConversationAdapter?.setActivateOnItemClick(activateOnItemClick)
        this.activateOnItemClick = activateOnItemClick
    }

    override fun showPreviousList() = if (backStack.isEmpty()) {
        false
    } else {
        currentLabel.value = backStack.pop()
        true
    }
}
