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
import android.view.*
import android.widget.Toast
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_BROADCAST
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_IDENTITY
import ch.dissem.apps.abit.adapter.SwipeableMessageAdapter
import ch.dissem.apps.abit.listener.ListSelectionListener
import ch.dissem.apps.abit.repository.AndroidMessageRepository
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.FabUtils
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.android.synthetic.main.fragment_message_list.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

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
class MessageListFragment : Fragment(), ListHolder<Label> {

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var swipeableMessageAdapter: SwipeableMessageAdapter? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private var recyclerViewTouchActionGuardManager: RecyclerViewTouchActionGuardManager? = null

    override var currentLabel: Label? = null

    private var emptyTrashMenuItem: MenuItem? = null
    private lateinit var messageRepo: AndroidMessageRepository
    private var activateOnItemClick: Boolean = false

    private val backStack = Stack<Label>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        val activity = activity as MainActivity
        initFab(activity)
        messageRepo = Singleton.getMessageRepository(activity)

        if (backStack.isEmpty()) {
            doUpdateList(activity.selectedLabel)
        } else {
            doUpdateList(backStack.peek())
        }
    }

    override fun updateList(label: Label) {
        if (currentLabel != null && currentLabel != label && currentLabel != backStack.peek()) {
            backStack.push(currentLabel)
        }
        if (!isResumed) {
            currentLabel = label
            return
        }

        doUpdateList(label)
    }

    private fun doUpdateList(label: Label?) {
        val mainActivity = activity as? MainActivity
        swipeableMessageAdapter?.clear(label)
        if (label == null) {
            mainActivity?.updateTitle(getString(R.string.app_name))
            swipeableMessageAdapter?.notifyDataSetChanged()
            return
        }
        currentLabel = label
        emptyTrashMenuItem?.isVisible = label.type == Label.Type.TRASH
        mainActivity?.apply {
            if ("archive" == label.toString()) {
                updateTitle(getString(R.string.archive))
            } else {
                updateTitle(label.toString())
            }
        }

        doAsync {
            messageRepo.findMessageIds(label)
                    .map { messageRepo.getMessage(it) }
                    .forEach { message ->
                        uiThread { swipeableMessageAdapter?.add(message) }
                    }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_message_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss
        // animation is running)
        val touchActionGuardManager = RecyclerViewTouchActionGuardManager().apply {
            setInterceptVerticalScrollingWhileAnimationRunning(true)
            isEnabled = true
        }

        // swipe manager
        val swipeManager = RecyclerViewSwipeManager()

        //swipeableMessageAdapter
        val adapter = SwipeableMessageAdapter().apply {
            setActivateOnItemClick(activateOnItemClick)
        }
        adapter.eventListener = object : SwipeableMessageAdapter.EventListener {
            override fun onItemDeleted(item: Plaintext) {
                if (MessageDetailFragment.isInTrash(item)) {
                    messageRepo.remove(item)
                } else {
                    item.labels.clear()
                    item.addLabels(messageRepo.getLabels(Label.Type.TRASH))
                    messageRepo.save(item)
                }
            }

            override fun onItemArchived(item: Plaintext) {
                item.labels.clear()
                messageRepo.save(item)
            }

            override fun onItemViewClicked(v: View?) {
                val position = recycler_view.getChildAdapterPosition(v)
                adapter.setSelectedPosition(position)
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.getItem(position)
                    (activity as MainActivity).onItemSelected(item)
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
        recycler_view.adapter = wrappedAdapter  // requires *wrapped* swipeableMessageAdapter
        recycler_view.itemAnimator = animator

        recycler_view.addItemDecoration(SimpleListDividerDecorator(
                ContextCompat.getDrawable(context, R.drawable.list_divider_h), true))

        // NOTE:
        // The initialization order is very important! This order determines the priority of
        // touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        touchActionGuardManager.attachRecyclerView(recycler_view)
        swipeManager.attachRecyclerView(recycler_view)

        recyclerViewTouchActionGuardManager = touchActionGuardManager
        recyclerViewSwipeManager = swipeManager
        this.swipeableMessageAdapter = adapter
    }

    private fun initFab(context: MainActivity) {
        val menu = FabSpeedDialMenu(context)
        menu.add(R.string.broadcast).setIcon(R.drawable.ic_action_broadcast)
        menu.add(R.string.personal_message).setIcon(R.drawable.ic_action_personal)
        FabUtils.initFab(context, R.drawable.ic_action_compose_message, menu)
                .addOnMenuItemClickListener { _, _, itemId ->
                    val identity = Singleton.getIdentity(activity)
                    if (identity == null) {
                        Toast.makeText(activity, R.string.no_identity_warning,
                                Toast.LENGTH_LONG).show()
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

        swipeableMessageAdapter = null
        layoutManager = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.message_list, menu)
        emptyTrashMenuItem = menu.findItem(R.id.empty_trash)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        currentLabel?.let { currentLabel ->
            when (item.itemId) {
                R.id.empty_trash -> {
                    if (currentLabel.type != Label.Type.TRASH) return true

                    doAsync {
                        for (message in messageRepo.findMessages(currentLabel)) {
                            messageRepo.remove(message)
                        }

                        uiThread { updateList(currentLabel) }
                    }
                    return true
                }
                else -> return false
            }
        }
        return false
    }

    override fun setActivateOnItemClick(activateOnItemClick: Boolean) {
        swipeableMessageAdapter?.setActivateOnItemClick(activateOnItemClick)
        this.activateOnItemClick = activateOnItemClick
    }

    override fun showPreviousList(): Boolean {
        return if (backStack.isEmpty()) {
            false
        } else {
            doUpdateList(backStack.pop())
            true
        }
    }
}
