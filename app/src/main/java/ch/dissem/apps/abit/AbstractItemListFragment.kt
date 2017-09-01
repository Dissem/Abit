/*
 * Copyright 2015 Christian Basler
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
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.View
import android.widget.ListView

import ch.dissem.apps.abit.listener.ListSelectionListener

/**
 * @author Christian Basler
 */
abstract class AbstractItemListFragment<L, T> : ListFragment(), ListHolder<L> {
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    @Suppress("UNCHECKED_CAST")
    private var callbacks: ListSelectionListener<T> = DummyCallback as ListSelectionListener<T>
    /**
     * The current activated item position. Only used on tablets.
     */
    private var activatedPosition = ListView.INVALID_POSITION
    private var activateOnItemClick: Boolean = false

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION))
        }
    }

    override fun onResume() {
        super.onResume()

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        listView.choiceMode = if (activateOnItemClick)
            ListView.CHOICE_MODE_SINGLE
        else
            ListView.CHOICE_MODE_NONE
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // Activities containing this fragment must implement its callbacks.
        if (context is ListSelectionListener<*>) {
            @Suppress("UNCHECKED_CAST")
            callbacks = context as ListSelectionListener<T>
        } else {
            throw IllegalStateException("Activity must implement fragment's callbacks.")
        }

    }

    override fun onDetach() {
        super.onDetach()

        // Reset the active callbacks interface to the dummy implementation.
        @Suppress("UNCHECKED_CAST")
        callbacks = DummyCallback as ListSelectionListener<T>
    }

    override fun onListItemClick(listView: ListView, view: View?, position: Int, id: Long) {
        super.onListItemClick(listView, view, position, id)

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        @Suppress("UNCHECKED_CAST")
        (listView.getItemAtPosition(position) as? T)?.let {
            callbacks.onItemSelected(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null && activatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition)
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    override fun setActivateOnItemClick(activateOnItemClick: Boolean) {
        this.activateOnItemClick = activateOnItemClick

        if (isVisible) {
            // When setting CHOICE_MODE_SINGLE, ListView will automatically
            // give items the 'activated' state when touched.
            listView.choiceMode = if (activateOnItemClick)
                ListView.CHOICE_MODE_SINGLE
            else
                ListView.CHOICE_MODE_NONE
        }
    }

    private fun setActivatedPosition(position: Int) {
        if (position == ListView.INVALID_POSITION) {
            listView.setItemChecked(activatedPosition, false)
        } else {
            listView.setItemChecked(position, true)
        }

        activatedPosition = position
    }

    override var currentLabel: L? = null

    override fun showPreviousList() = false

    /**
     * A dummy implementation of the [ListSelectionListener] interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    internal object DummyCallback : ListSelectionListener<Any> {
        override fun onItemSelected(item: Any) {
            // NO OP
        }
    }

    companion object {
        /**
         * The serialization (saved instance state) Bundle key representing the
         * activated item position. Only used on tablets.
         */
        internal const val STATE_ACTIVATED_POSITION = "activated_position"
    }
}
