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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import ch.dissem.apps.abit.adapter.ConversationAdapter
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Drawables
import ch.dissem.bitmessage.entity.Conversation
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import kotlinx.android.synthetic.main.fragment_conversation_detail.*
import java.util.*

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
    private var itemId: UUID? = null
    private var item: Conversation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { arguments ->
            if (arguments.containsKey(ARG_ITEM_ID)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                itemId = arguments.getSerializable(ARG_ITEM_ID) as UUID
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

        item = itemId?.let { Singleton.getConversationService(ctx).getConversation(it) }

        // Show the dummy content as text in a TextView.
        item?.let { item ->
            subject.text = item.subject
            avatar.setImageDrawable(MultiIdenticon(item.participants))
            messages.adapter =
                ConversationAdapter(ctx, this@ConversationDetailFragment, item, Singleton.currentLabel.value)
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

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }
}
