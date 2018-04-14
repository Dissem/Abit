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
import android.graphics.Point
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import ch.dissem.apps.abit.drawer.ProfileImageListener
import ch.dissem.apps.abit.drawer.ProfileSelectionListener
import ch.dissem.apps.abit.listener.ListSelectionListener
import ch.dissem.apps.abit.repository.AndroidLabelRepository.Companion.LABEL_ARCHIVE
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.service.Singleton.currentLabel
import ch.dissem.apps.abit.synchronization.SyncAdapter
import ch.dissem.apps.abit.util.NetworkUtils
import ch.dissem.apps.abit.util.Preferences
import ch.dissem.apps.abit.util.getColor
import ch.dissem.apps.abit.util.getIcon
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.Conversation
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.Label
import com.github.amlcurran.showcaseview.ShowcaseView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import io.github.kobakei.materialfabspeeddial.FabSpeedDial
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.*


/**
 * An activity representing a list of Messages. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [MessageDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 *
 *
 * The activity makes heavy use of fragments. The list of items is a
 * [MessageListFragment] and the item details
 * (if present) is a [MessageDetailFragment].
 *
 *
 * This activity also implements the required
 * [ListSelectionListener] interface
 * to listen for item selections.
 *
 */
class MainActivity : AppCompatActivity(), ListSelectionListener<Serializable> {

    private var active: Boolean = false

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    var hasDetailPane: Boolean = false
        private set

    private lateinit var bmc: BitmessageContext
    private lateinit var accountHeader: AccountHeader

    private lateinit var drawer: Drawer
    private lateinit var nodeSwitch: SwitchDrawerItem

    val floatingActionButton: FabSpeedDial?
        get() = findViewById(R.id.fab)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)
        bmc = Singleton.getBitmessageContext(this)

        setContentView(R.layout.activity_main)
        fab.hide()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val listFragment = ConversationListFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.item_list, listFragment)
            .commit()

        if (findViewById<View>(R.id.message_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            hasDetailPane = true

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            listFragment.setActivateOnItemClick(true)
        }

        createDrawer(toolbar)

        // handle intents
        val intent = intent
        if (intent.hasExtra(EXTRA_SHOW_MESSAGE)) {
            onItemSelected(intent.getSerializableExtra(EXTRA_SHOW_MESSAGE))
        }
        if (intent.hasExtra(EXTRA_REPLY_TO_MESSAGE)) {
            val item = intent.getSerializableExtra(EXTRA_REPLY_TO_MESSAGE) as Plaintext
            ComposeMessageActivity.launchReplyTo(this, item)
        }

        if (Preferences.useTrustedNode(this)) {
            SyncAdapter.startSync(this)
        } else {
            SyncAdapter.stopSync(this)
        }
        if (drawer.isDrawerOpen) {
            val lps = RelativeLayout.LayoutParams(
                ViewGroup
                    .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                val margin = ((resources.displayMetrics.density * 12) as Number).toInt()
                setMargins(margin, margin, margin, margin)
            }

            ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle(R.string.full_node)
                .setContentText(R.string.full_node_description)
                .setTarget {
                    val view = drawer.stickyFooter
                    val location = IntArray(2)
                    view.getLocationInWindow(location)
                    val x = location[0] + 7 * view.width / 8
                    val y = location[1] + view.height / 2
                    Point(x, y)
                }
                .replaceEndButton(R.layout.showcase_button)
                .hideOnTouchOutside()
                .build()
                .setButtonPosition(lps)
        }
    }

    private fun <F> changeList(listFragment: F) where F : Fragment, F : ListHolder<*> {
        if (active) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.item_list, listFragment)
            supportFragmentManager.findFragmentById(R.id.message_detail_container)?.let {
                transaction.remove(it)
            }
            transaction.addToBackStack(null).commit()

            if (hasDetailPane) {
                // In two-pane mode, list items should be given the
                // 'activated' state when touched.
                listFragment.setActivateOnItemClick(true)
            }
        }
    }

    private fun createDrawer(toolbar: Toolbar) {
        val profiles = ArrayList<IProfile<*>>()
        profiles.add(
            ProfileSettingDrawerItem()
                .withName(getString(R.string.add_identity))
                .withDescription(getString(R.string.add_identity_summary))
                .withIcon(
                    IconicsDrawable(this, GoogleMaterial.Icon.gmd_add)
                        .actionBar()
                        .paddingDp(5)
                        .colorRes(R.color.icons)
                )
                .withIdentifier(ADD_IDENTITY.toLong())
        )
        profiles.add(
            ProfileSettingDrawerItem()
                .withName(getString(R.string.manage_identity))
                .withIcon(GoogleMaterial.Icon.gmd_settings)
                .withIdentifier(MANAGE_IDENTITY.toLong())
        )
        // Create the AccountHeader
        accountHeader = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.header)
            .withProfiles(profiles)
            .withOnAccountHeaderProfileImageListener(ProfileImageListener(this))
            .withOnAccountHeaderListener(
                ProfileSelectionListener(
                    this@MainActivity,
                    supportFragmentManager
                )
            )
            .build()
        if (profiles.size > 2) { // There's always the add and manage identity items
            accountHeader.setActiveProfile(profiles[0], true)
        }

        val drawerItems = ArrayList<IDrawerItem<*, *>>()
        drawerItems.add(
            PrimaryDrawerItem()
                .withIdentifier(LABEL_ARCHIVE.id as Long)
                .withName(R.string.archive)
                .withTag(LABEL_ARCHIVE)
                .withIcon(CommunityMaterial.Icon.cmd_archive)
        )
        drawerItems.add(DividerDrawerItem())
        drawerItems.add(
            PrimaryDrawerItem()
                .withName(R.string.contacts_and_subscriptions)
                .withIcon(GoogleMaterial.Icon.gmd_contacts)
        )
        drawerItems.add(
            PrimaryDrawerItem()
                .withName(R.string.settings)
                .withIcon(GoogleMaterial.Icon.gmd_settings)
        )

        nodeSwitch = SwitchDrawerItem()
            .withIdentifier(ID_NODE_SWITCH)
            .withName(R.string.full_node)
            .withIcon(CommunityMaterial.Icon.cmd_cloud_outline)
            .withChecked(Preferences.isFullNodeActive(this))
            .withOnCheckedChangeListener { _, _, isChecked ->
                if (isChecked) {
                    NetworkUtils.enableNode(this@MainActivity)
                } else {
                    NetworkUtils.disableNode(this@MainActivity)
                }
            }

        drawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withAccountHeader(accountHeader)
            .withDrawerItems(drawerItems)
            .addStickyDrawerItems(nodeSwitch)
            .withOnDrawerItemClickListener(DrawerItemClickListener())
            .withShowDrawerOnFirstLaunch(true)
            .build()

        loadDrawerItemsAsynchronously()
    }

    private fun loadDrawerItemsAsynchronously() {
        doAsync {
            val identities = bmc.addresses.getIdentities()
            if (identities.isEmpty()) {
                // Create an initial identity
                Singleton.getIdentity(this@MainActivity)
            }

            uiThread {
                for (identity in identities) {
                    addIdentityEntry(identity)
                }
            }
        }

        doAsync {
            val labels = bmc.labels.getLabels()

            uiThread {
                if (intent.hasExtra(EXTRA_SHOW_LABEL)) {
                    currentLabel.value = intent.getSerializableExtra(EXTRA_SHOW_LABEL) as Label
                } else if (currentLabel.value == null) {
                    currentLabel.value = labels[0]

                }
                for (label in labels) {
                    addLabelEntry(label)
                }
                currentLabel.value?.let {
                    drawer.setSelection(it.id as Long)
                }
                updateUnread()
            }
        }
    }

    override fun onBackPressed() {
        val listFragment = supportFragmentManager.findFragmentById(R.id.item_list)
        if (listFragment !is ListHolder<*> || !listFragment.showPreviousList()) {
            super.onBackPressed()
        }
    }

    private inner class DrawerItemClickListener : Drawer.OnDrawerItemClickListener {
        override fun onItemClick(view: View?, position: Int, item: IDrawerItem<*, *>): Boolean {
            val itemList = supportFragmentManager.findFragmentById(R.id.item_list)
            val tag = item.tag
            if (tag is Label) {
                currentLabel.value = tag
                if (tag.type == Label.Type.INBOX || tag == LABEL_ARCHIVE) {
                    if (itemList !is ConversationListFragment) {
                        changeList(ConversationListFragment())
                    }
                } else {
                    if (itemList !is MessageListFragment) {
                        changeList(MessageListFragment())
                    }
                }
                return false
            } else if (item is Nameable<*>) {
                when (item.name.textRes) {
                    R.string.contacts_and_subscriptions -> {
                        if (itemList is AddressListFragment) {
                            itemList.updateList()
                        } else {
                            changeList(AddressListFragment())
                        }
                        return false
                    }
                    R.string.settings -> {
                        supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.item_list, SettingsFragment())
                            .addToBackStack(null)
                            .commit()
                        return false
                    }
                    R.string.full_node -> return true
                    else -> return false
                }
            }
            return false
        }
    }

    override fun onResume() {
        updateUnread()
        if (Preferences.isFullNodeActive(this) && Preferences.isConnectionAllowed(this@MainActivity)) {
            NetworkUtils.enableNode(this, false)
        }
        Singleton.getMessageListener(this).resetNotification()
        currentLabel.addObserver(this) { label ->
            if (label != null && label.id is Long) {
                drawer.setSelection(label.id as Long)
            }
        }
        active = true
        super.onResume()
    }

    override fun onPause() {
        currentLabel.removeObserver(this)
        super.onPause()
        active = false
    }

    fun addIdentityEntry(identity: BitmessageAddress) {
        val newProfile = ProfileDrawerItem()
            .withIcon(Identicon(identity))
            .withName(identity.toString())
            .withNameShown(true)
            .withEmail(identity.address)
            .withTag(identity)
        if (accountHeader.profiles != null) {
            // we know that there are 2 setting elements.
            // Set the new profile above them ;)
            accountHeader.addProfile(
                newProfile, accountHeader.profiles.size - 2
            )
        } else {
            accountHeader.addProfiles(newProfile)
        }
    }

    private fun addLabelEntry(label: Label) {
        val item = PrimaryDrawerItem()
            .withIdentifier(label.id as Long)
            .withName(label.toString())
            .withTag(label)
            .withIcon(label.getIcon())
            .withIconColor(label.getColor(0xFF000000.toInt()))
        drawer.addItemAtPosition(item, drawer.drawerItems.size - 3)
    }

    fun updateIdentityEntry(identity: BitmessageAddress) {
        for (profile in accountHeader.profiles) {
            if (profile is ProfileDrawerItem) {
                if (identity == profile.tag) {
                    profile
                        .withName(identity.toString())
                        .withTag(identity)
                    return
                }
            }
        }
    }

    fun removeIdentityEntry(identity: BitmessageAddress) {
        for (profile in accountHeader.profiles) {
            if (profile is ProfileDrawerItem) {
                if (identity == profile.tag) {
                    accountHeader.removeProfile(profile)
                    return
                }
            }
        }
    }

    fun updateUnread() {
        for (item in drawer.drawerItems) {
            if (item.tag is Label) {
                val label = item.tag as Label
                if (label !== LABEL_ARCHIVE) {
                    val unread = bmc.messages.countUnread(label)
                    if (unread > 0) {
                        (item as PrimaryDrawerItem).withBadge(unread.toString())
                    } else {
                        (item as PrimaryDrawerItem).withBadge(null as String?)
                    }
                    runOnUiThread {
                        drawer.updateItem(item)
                    }
                }
            }
        }
    }

    /**
     * Callback method from [ListSelectionListener]
     * indicating that the item with the given ID was selected.
     */
    override fun onItemSelected(item: Serializable) {
        if (hasDetailPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            val fragment = when (item) {
                is Conversation -> {
                    ConversationDetailFragment().apply {
                        arguments = Bundle().apply {
                            putSerializable(ConversationDetailFragment.ARG_ITEM, item)
                        }
                    }
                }
                is Plaintext -> {
                    if (item.labels.any { it.type == Label.Type.DRAFT }) {
                        ComposeMessageFragment().apply {
                            arguments = Bundle().apply {
                                putSerializable(ComposeMessageActivity.EXTRA_DRAFT, item)
                            }
                        }
                    } else {
                        MessageDetailFragment().apply {
                            arguments = Bundle().apply {
                                putSerializable(MessageDetailFragment.ARG_ITEM, item)
                            }
                        }
                    }
                }
                is BitmessageAddress -> {
                    AddressDetailFragment().apply {
                        arguments = Bundle().apply {
                            putSerializable(AddressDetailFragment.ARG_ITEM, item)
                        }
                    }
                }
                else -> throw IllegalArgumentException("Plaintext or BitmessageAddress expected, but was ${item::class.simpleName}")
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.message_detail_container, fragment)
                .commit()
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            val detailIntent = when (item) {
                is Conversation -> {
                    Intent(this, MessageDetailActivity::class.java).apply {
                        putExtra(ConversationDetailFragment.ARG_ITEM, item)
                    }
                }
                is Plaintext -> {
                    if (item.labels.any { it.type == Label.Type.DRAFT }) {
                        Intent(this, ComposeMessageActivity::class.java).apply {
                            putExtra(ComposeMessageActivity.EXTRA_DRAFT, item)
                        }
                    } else {
                        Intent(this, MessageDetailActivity::class.java).apply {
                            putExtra(MessageDetailFragment.ARG_ITEM, item)
                        }
                    }
                }
                is BitmessageAddress -> Intent(this, AddressDetailActivity::class.java).apply {
                    putExtra(AddressDetailFragment.ARG_ITEM, item)
                }
                else -> throw IllegalArgumentException("Plaintext or BitmessageAddress expected, but was ${item::class.simpleName}")
            }
            startActivity(detailIntent)
        }
    }

    fun setDetailView(fragment: Fragment) {
        if (hasDetailPane) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.message_detail_container, fragment)
                .commit()
        }
    }

    fun updateTitle(title: CharSequence) {
        supportActionBar?.title = title
    }

    fun initFab(@DrawableRes drawableRes: Int, menu: FabSpeedDialMenu): FabSpeedDial {
        val fab = floatingActionButton ?: throw IllegalStateException("Fab must not be null")
        fab.removeAllOnMenuItemClickListeners()
        fab.show()
        fab.closeMenu()
        val mainFab = fab.mainFab
        mainFab.setImageResource(drawableRes)
        fab.setMenu(menu)
        fab.addOnStateChangeListener { isOpened: Boolean ->
            if (isOpened) {
                // It will be turned 45 degrees, which makes an x out of the +
                mainFab.setImageResource(R.drawable.ic_action_add)
            } else {
                mainFab.setImageResource(drawableRes)
            }
        }
        return fab
    }

    companion object {
        const val EXTRA_SHOW_MESSAGE = "ch.dissem.abit.ShowMessage"
        const val EXTRA_SHOW_LABEL = "ch.dissem.abit.ShowLabel"
        const val EXTRA_REPLY_TO_MESSAGE = "ch.dissem.abit.ReplyToMessage"
        const val ACTION_SHOW_INBOX = "ch.dissem.abit.ShowInbox"

        const val ADD_IDENTITY = 1
        const val MANAGE_IDENTITY = 2

        private const val ID_NODE_SWITCH: Long = 1

        private var instance: WeakReference<MainActivity>? = null

        fun updateNodeSwitch() {
            apply {
                runOnUiThread {
                    nodeSwitch.withChecked(Preferences.isFullNodeActive(this))
                    drawer.updateStickyFooterItem(nodeSwitch)
                }
            }
        }

        /**
         * Runs the given code in the main activity context, if it currently exists. Otherwise,
         * it's ignored.
         */
        fun apply(run: MainActivity.() -> Unit) {
            instance?.get()?.let { run.invoke(it) }
        }
    }
}
