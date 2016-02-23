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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.mikepenz.materialdrawer.model.interfaces.OnCheckedChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.listener.ListSelectionListener;
import ch.dissem.apps.abit.service.BitmessageService;
import ch.dissem.apps.abit.service.BitmessageService.BitmessageBinder;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.synchronization.SyncAdapter;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import static ch.dissem.apps.abit.service.BitmessageService.isRunning;


/**
 * An activity representing a list of Messages. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MessageDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link MessageListFragment} and the item details
 * (if present) is a {@link MessageDetailFragment}.
 * </p><p>
 * This activity also implements the required
 * {@link ListSelectionListener} interface
 * to listen for item selections.
 * </p>
 */
public class MainActivity extends AppCompatActivity
        implements ListSelectionListener<Serializable>, ActionBarListener {
    public static final String EXTRA_SHOW_MESSAGE = "ch.dissem.abit.ShowMessage";
    public static final String ACTION_SHOW_INBOX = "ch.dissem.abit.ShowInbox";

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);
    private static final int ADD_IDENTITY = 1;
    private static final int MANAGE_IDENTITY = 2;

    public static WeakReference<MainActivity> instance;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean twoPane;

    private static BitmessageBinder service;
    private static boolean bound;
    private static ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.service = (BitmessageBinder) service;
            MainActivity.bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            bound = false;
        }
    };

    private Label selectedLabel;

    private BitmessageContext bmc;
    private AccountHeader accountHeader;

    private Drawer drawer;
    private ShowcaseView showcaseView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bmc = Singleton.getBitmessageContext(this);
        List<Label> labels = bmc.messages().getLabels();
        if (selectedLabel == null) {
            selectedLabel = labels.get(0);
        }

        setContentView(R.layout.activity_message_list);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MessageListFragment listFragment = new MessageListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.item_list, listFragment)
                .commit();

        if (findViewById(R.id.message_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            listFragment.setActivateOnItemClick(true);
        }

        createDrawer(toolbar, labels);

        Singleton.getMessageListener(this).resetNotification();

        // handle intents
        if (getIntent().hasExtra(EXTRA_SHOW_MESSAGE)) {
            onItemSelected(getIntent().getSerializableExtra(EXTRA_SHOW_MESSAGE));
        }

        if (Preferences.useTrustedNode(this)) {
            SyncAdapter.startSync(this);
        } else {
            SyncAdapter.stopSync(this);
        }
        if (drawer.isDrawerOpen()) {
            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup
                    .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
            lps.setMargins(margin, margin, margin, margin);

            showcaseView = new ShowcaseView.Builder(this)
                    .withMaterialShowcase()
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setContentTitle(R.string.full_node)
                    .setContentText(R.string.full_node_description)
                    .setTarget(new Target() {
                                   @Override
                                   public Point getPoint() {
                                       View view = drawer.getStickyFooter();
                                       int[] location = new int[2];
                                       view.getLocationInWindow(location);
                                       int x = location[0] + 7 * view.getWidth() / 8;
                                       int y = location[1] + view.getHeight() / 2;
                                       return new Point(x, y);
                                   }
                               }
                    )
                    .replaceEndButton(R.layout.showcase_button)
                    .hideOnTouchOutside()
                    .build();
            showcaseView.setButtonPosition(lps);
        }
    }

    private void changeList(AbstractItemListFragment<?> listFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.item_list, listFragment)
                .addToBackStack(null)
                .commit();

        if (twoPane) {
            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            listFragment.setActivateOnItemClick(true);
        }
    }

    private void createDrawer(Toolbar toolbar, Collection<Label> labels) {
        final ArrayList<IProfile> profiles = new ArrayList<>();
        for (BitmessageAddress identity : bmc.addresses().getIdentities()) {
            LOG.info("Adding identity " + identity.getAddress());
            profiles.add(new ProfileDrawerItem()
                            .withIcon(new Identicon(identity))
                            .withName(identity.toString())
                            .withNameShown(true)
                            .withEmail(identity.getAddress())
                            .withTag(identity)
            );
        }
        if (profiles.isEmpty()) {
            // Create an initial identity
            BitmessageAddress identity = Singleton.getIdentity(this);
            profiles.add(new ProfileDrawerItem()
                            .withIcon(new Identicon(identity))
                            .withName(identity.toString())
                            .withEmail(identity.getAddress())
                            .withTag(identity)
            );
        }
        profiles.add(new ProfileSettingDrawerItem()
                        .withName("Add Identity")
                        .withDescription("Create new identity")
                        .withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_add)
                                .actionBar()
                                .paddingDp(5)
                                .colorRes(R.color.icons))
                        .withIdentifier(ADD_IDENTITY)
        );
        profiles.add(new ProfileSettingDrawerItem()
                        .withName(getString(R.string.manage_identity))
                        .withIcon(GoogleMaterial.Icon.gmd_settings)
                        .withIdentifier(MANAGE_IDENTITY)
        );
        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withProfiles(profiles)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean
                            currentProfile) {
                        switch (profile.getIdentifier()) {
                            case ADD_IDENTITY:
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(R.string.add_identity_warning)
                                        .setPositiveButton(android.R.string.yes, new
                                                DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog,
                                                                        int which) {
                                                        BitmessageAddress identity = bmc
                                                                .createIdentity(false);
                                                        IProfile newProfile = new
                                                                ProfileDrawerItem()
                                                                .withName(identity.toString())
                                                                .withEmail(identity.getAddress())
                                                                .withTag(identity);
                                                        if (accountHeader.getProfiles() != null) {
                                                            // we know that there are 2 setting
                                                            // elements.
                                                            // Set the new profile above them ;)
                                                            accountHeader.addProfile(
                                                                    newProfile, accountHeader
                                                                            .getProfiles().size()
                                                                            - 2);
                                                        } else {
                                                            accountHeader.addProfiles(newProfile);
                                                        }
                                                    }
                                                })
                                        .setNegativeButton(android.R.string.no, null)
                                        .show();
                                break;
                            case MANAGE_IDENTITY:
                                Intent show = new Intent(MainActivity.this,
                                        AddressDetailActivity.class);
                                show.putExtra(AddressDetailFragment.ARG_ITEM,
                                        Singleton.getIdentity(getApplicationContext()));
                                startActivity(show);
                                break;
                            default:
                                if (profile instanceof ProfileDrawerItem) {
                                    Object tag = ((ProfileDrawerItem) profile).getTag();
                                    if (tag instanceof BitmessageAddress) {
                                        Singleton.setIdentity((BitmessageAddress) tag);
                                    }
                                }
                        }
                        // false if it should close the drawer
                        return false;
                    }
                })
                .build();
        if (profiles.size() > 2) { // There's always the add and manage identity items
            accountHeader.setActiveProfile(profiles.get(0), true);
        }

        ArrayList<IDrawerItem> drawerItems = new ArrayList<>();
        for (Label label : labels) {
            PrimaryDrawerItem item = new PrimaryDrawerItem().withName(label.toString()).withTag
                    (label);
            if (label.getType() == null) {
                item.withIcon(CommunityMaterial.Icon.cmd_label);
            } else {
                switch (label.getType()) {
                    case INBOX:
                        item.withIcon(GoogleMaterial.Icon.gmd_inbox);
                        break;
                    case DRAFT:
                        item.withIcon(CommunityMaterial.Icon.cmd_file);
                        break;
                    case SENT:
                        item.withIcon(CommunityMaterial.Icon.cmd_send);
                        break;
                    case BROADCAST:
                        item.withIcon(CommunityMaterial.Icon.cmd_rss);
                        break;
                    case UNREAD:
                        item.withIcon(GoogleMaterial.Icon.gmd_markunread_mailbox);
                        break;
                    case TRASH:
                        item.withIcon(GoogleMaterial.Icon.gmd_delete);
                        break;
                    default:
                        item.withIcon(CommunityMaterial.Icon.cmd_label);
                }
            }
            drawerItems.add(item);
        }
        drawerItems.add(new PrimaryDrawerItem()
                        .withName(R.string.archive)
                        .withTag(null)
                        .withIcon(CommunityMaterial.Icon.cmd_archive)
        );
        drawerItems.add(new DividerDrawerItem());
        drawerItems.add(new PrimaryDrawerItem()
                .withName(R.string.contacts_and_subscriptions)
                .withIcon(GoogleMaterial.Icon.gmd_contacts));
        drawerItems.add(new PrimaryDrawerItem()
                .withName(R.string.settings)
                .withIcon(GoogleMaterial.Icon.gmd_settings));

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .withDrawerItems(drawerItems)
                .addStickyDrawerItems(
                        new SwitchDrawerItem()
                                .withName(R.string.full_node)
                                .withIcon(CommunityMaterial.Icon.cmd_cloud_outline)
                                .withChecked(isRunning())
                                .withOnCheckedChangeListener(new OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(IDrawerItem drawerItem,
                                                                 CompoundButton buttonView,
                                                                 boolean isChecked) {
                                        if (isChecked) {
                                            checkAndStartNode(buttonView);
                                        } else {
                                            service.shutdownNode();
                                        }
                                    }
                                })
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> adapterView, View view, int i, long
                            l, IDrawerItem item) {
                        if (item.getTag() instanceof Label) {
                            selectedLabel = (Label) item.getTag();
                            showSelectedLabel();
                            return false;
                        } else if (item instanceof Nameable<?>) {
                            Nameable<?> ni = (Nameable<?>) item;
                            switch (ni.getNameRes()) {
                                case R.string.contacts_and_subscriptions:
                                    if (!(getSupportFragmentManager().findFragmentById(R.id
                                            .item_list) instanceof AddressListFragment)) {
                                        changeList(new AddressListFragment());
                                    } else {
                                        ((AddressListFragment) getSupportFragmentManager()
                                                .findFragmentById(R.id.item_list)).updateList();
                                    }

                                    break;
                                case R.string.settings:
                                    startActivity(new Intent(MainActivity.this, SettingsActivity
                                            .class));
                                    break;
                                case R.string.archive:
                                    selectedLabel = null;
                                    showSelectedLabel();
                                    break;
                                case R.string.full_node:
                                    return true;
                            }
                        }
                        return false;
                    }
                })
                .withShowDrawerOnFirstLaunch(true)
                .build();
    }

    @Override
    protected void onResume() {
        instance = new WeakReference<>(this);
        updateUnread();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance = null;
    }

    private void checkAndStartNode(final CompoundButton buttonView) {
        if (service == null) return;

        if (Preferences.isConnectionAllowed(MainActivity.this)) {
            service.startupNode();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.full_node_warning)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            service.startupNode();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            buttonView.setChecked(false);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void updateUnread() {
        for (IDrawerItem item : drawer.getDrawerItems()) {
            if (item.getTag() instanceof Label) {
                Label label = (Label) item.getTag();
                int unread = bmc.messages().countUnread(label);
                if (unread > 0) {
                    ((PrimaryDrawerItem) item).withBadge(String.valueOf(unread));
                } else {
                    ((PrimaryDrawerItem) item).withBadge(null);
                }
            }
        }
    }

    private void showSelectedLabel() {
        if (getSupportFragmentManager().findFragmentById(R.id.item_list) instanceof
                MessageListFragment) {
            ((MessageListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list)).updateList(selectedLabel);
        } else {
            MessageListFragment listFragment = new MessageListFragment();
            changeList(listFragment);
            listFragment.updateList(selectedLabel);
        }
    }

    /**
     * Callback method from {@link ListSelectionListener}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Serializable item) {
        if (twoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putSerializable(MessageDetailFragment.ARG_ITEM, item);
            Fragment fragment;
            if (item instanceof Plaintext)
                fragment = new MessageDetailFragment();
            else if (item instanceof BitmessageAddress)
                fragment = new AddressDetailFragment();
            else
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but " +
                        "was "
                        + item.getClass().getSimpleName());
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.message_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent;
            if (item instanceof Plaintext)
                detailIntent = new Intent(this, MessageDetailActivity.class);
            else if (item instanceof BitmessageAddress)
                detailIntent = new Intent(this, AddressDetailActivity.class);
            else
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but " +
                        "was "
                        + item.getClass().getSimpleName());

            detailIntent.putExtra(MessageDetailFragment.ARG_ITEM, item);
            startActivity(detailIntent);
        }
    }

    @Override
    public void updateTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    public Label getSelectedLabel() {
        return selectedLabel;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BitmessageService.class), connection, Context
                .BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        super.onStop();
    }

    public static MainActivity getInstance() {
        if (instance == null) return null;
        return instance.get();
    }
}
