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

import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ch.dissem.apps.abit.dialog.FullNodeDialogActivity;
import ch.dissem.apps.abit.drawer.ProfileImageListener;
import ch.dissem.apps.abit.drawer.ProfileSelectionListener;
import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.listener.ListSelectionListener;
import ch.dissem.apps.abit.service.BitmessageService;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.synchronization.SyncAdapter;
import ch.dissem.apps.abit.util.Labels;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import static ch.dissem.apps.abit.ComposeMessageActivity.launchReplyTo;
import static ch.dissem.apps.abit.repository.AndroidMessageRepository.LABEL_ARCHIVE;
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
    public static final String EXTRA_SHOW_LABEL = "ch.dissem.abit.ShowLabel";
    public static final String EXTRA_REPLY_TO_MESSAGE = "ch.dissem.abit.ReplyToMessage";
    public static final String ACTION_SHOW_INBOX = "ch.dissem.abit.ShowInbox";

    public static final int ADD_IDENTITY = 1;
    public static final int MANAGE_IDENTITY = 2;

    private static final long ID_NODE_SWITCH = 1;

    private static WeakReference<MainActivity> instance;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean twoPane;

    private Label selectedLabel;

    private BitmessageContext bmc;
    private AccountHeader accountHeader;

    private Drawer drawer;
    private SwitchDrawerItem nodeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = new WeakReference<>(this);
        bmc = Singleton.getBitmessageContext(this);

        setContentView(R.layout.activity_message_list);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MessageListFragment listFragment = new MessageListFragment();
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.item_list, listFragment)
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

        createDrawer(toolbar);

        // handle intents
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SHOW_MESSAGE)) {
            onItemSelected(intent.getSerializableExtra(EXTRA_SHOW_MESSAGE));
        }
        if (intent.hasExtra(EXTRA_REPLY_TO_MESSAGE)) {
            Plaintext item = (Plaintext) intent.getSerializableExtra(EXTRA_REPLY_TO_MESSAGE);
            launchReplyTo(this, item);
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

            new ShowcaseView.Builder(this)
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
                })
                .replaceEndButton(R.layout.showcase_button)
                .hideOnTouchOutside()
                .build()
                .setButtonPosition(lps);
        }
    }

    private <F extends Fragment & ListHolder> void changeList(F listFragment) {
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

    private void createDrawer(Toolbar toolbar) {
        final ArrayList<IProfile> profiles = new ArrayList<>();
        profiles.add(new ProfileSettingDrawerItem()
            .withName(getString(R.string.add_identity))
            .withDescription(getString(R.string.add_identity_summary))
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
            .withOnAccountHeaderProfileImageListener(new ProfileImageListener(this))
            .withOnAccountHeaderListener(new ProfileSelectionListener(MainActivity.this, getSupportFragmentManager()))
            .build();
        if (profiles.size() > 2) { // There's always the add and manage identity items
            accountHeader.setActiveProfile(profiles.get(0), true);
        }

        final ArrayList<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(new PrimaryDrawerItem()
            .withName(R.string.archive)
            .withTag(LABEL_ARCHIVE)
            .withIcon(CommunityMaterial.Icon.cmd_archive)
        );
        drawerItems.add(new DividerDrawerItem());
        drawerItems.add(new PrimaryDrawerItem()
            .withName(R.string.contacts_and_subscriptions)
            .withIcon(GoogleMaterial.Icon.gmd_contacts));
        drawerItems.add(new PrimaryDrawerItem()
            .withName(R.string.settings)
            .withIcon(GoogleMaterial.Icon.gmd_settings));

        nodeSwitch = new SwitchDrawerItem()
            .withIdentifier(ID_NODE_SWITCH)
            .withName(R.string.full_node)
            .withIcon(CommunityMaterial.Icon.cmd_cloud_outline)
            .withChecked(isRunning())
            .withOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView,
                                             boolean isChecked) {
                    Preferences.setFullNodeActive(MainActivity.this, isChecked);
                    if (isChecked) {
                        checkAndStartNode();
                    } else {
                        stopService(new Intent(MainActivity.this, BitmessageService.class));
                    }
                }
            });

        drawer = new DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withAccountHeader(accountHeader)
            .withDrawerItems(drawerItems)
            .addStickyDrawerItems(nodeSwitch)
            .withOnDrawerItemClickListener(new DrawerItemClickListener())
            .withShowDrawerOnFirstLaunch(true)
            .build();

        loadDrawerItemsAsynchronously();
    }

    private void loadDrawerItemsAsynchronously() {
        new AsyncTask<Void, Void, List<BitmessageAddress>>() {
            @Override
            protected List<BitmessageAddress> doInBackground(Void... params) {
                List<BitmessageAddress> identities = bmc.addresses().getIdentities();
                if (identities.isEmpty()) {
                    // Create an initial identity
                    Singleton.getIdentity(MainActivity.this);
                }
                return identities;
            }

            @Override
            protected void onPostExecute(List<BitmessageAddress> identities) {
                for (BitmessageAddress identity : identities) {
                    addIdentityEntry(identity);
                }
            }
        }.execute();

        new AsyncTask<Void, Void, List<Label>>() {
            @Override
            protected List<Label> doInBackground(Void... params) {
                return bmc.messages().getLabels();
            }

            @Override
            protected void onPostExecute(List<Label> labels) {
                if (getIntent().hasExtra(EXTRA_SHOW_LABEL)) {
                    selectedLabel = (Label) getIntent().getSerializableExtra(EXTRA_SHOW_LABEL);
                } else if (selectedLabel == null) {
                    selectedLabel = labels.get(0);
                }
                for (Label label : labels) {
                    addLabelEntry(label);
                }
                IDrawerItem selectedDrawerItem = drawer.getDrawerItem(selectedLabel);
                if (selectedDrawerItem != null) {
                    drawer.setSelection(selectedDrawerItem);
                }
            }
        }.execute();
    }

    private class DrawerItemClickListener implements Drawer.OnDrawerItemClickListener {
        @Override
        public boolean onItemClick(View view, int position, IDrawerItem item) {
            if (item.getTag() instanceof Label) {
                selectedLabel = (Label) item.getTag();
                if (getSupportFragmentManager().findFragmentById(R.id.item_list) instanceof
                    MessageListFragment) {
                    ((MessageListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.item_list)).updateList(selectedLabel);
                } else {
                    MessageListFragment listFragment = new MessageListFragment();
                    changeList(listFragment);
                    listFragment.updateList(selectedLabel);
                }
                return false;
            } else if (item instanceof Nameable<?>) {
                Nameable<?> ni = (Nameable<?>) item;
                switch (ni.getName().getTextRes()) {
                    case R.string.contacts_and_subscriptions:
                        if (!(getSupportFragmentManager().findFragmentById(R.id
                            .item_list) instanceof AddressListFragment)) {
                            changeList(new AddressListFragment());
                        } else {
                            ((AddressListFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.item_list)).updateList();
                        }
                        return false;
                    case R.string.settings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity
                            .class));
                        return false;
                    case R.string.full_node:
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("selectedLabel", selectedLabel);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        selectedLabel = (Label) savedInstanceState.getSerializable("selectedLabel");

        IDrawerItem selectedItem = drawer.getDrawerItem(selectedLabel);
        if (selectedItem != null) {
            drawer.setSelection(selectedItem);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        updateUnread();
        if (Preferences.isFullNodeActive(this) && Preferences.isConnectionAllowed(MainActivity.this)) {
            startService(new Intent(this, BitmessageService.class));
        }
        updateNodeSwitch();
        Singleton.getMessageListener(this).resetNotification();
        super.onResume();
    }

    public void addIdentityEntry(BitmessageAddress identity) {
        IProfile newProfile = new ProfileDrawerItem()
            .withIcon(new Identicon(identity))
            .withName(identity.toString())
            .withNameShown(true)
            .withEmail(identity.getAddress())
            .withTag(identity);
        if (accountHeader.getProfiles() != null) {
            // we know that there are 2 setting elements.
            // Set the new profile above them ;)
            accountHeader.addProfile(
                newProfile, accountHeader.getProfiles().size() - 2);
        } else {
            accountHeader.addProfiles(newProfile);
        }
    }

    public void addLabelEntry(Label label) {
        PrimaryDrawerItem item = new PrimaryDrawerItem()
            .withName(label.toString())
            .withTag(label)
            .withIcon(Labels.getIcon(label))
            .withIconColor(Labels.getColor(label));
        drawer.addItemAtPosition(item, drawer.getDrawerItems().size() - 3);
    }

    public void updateIdentityEntry(BitmessageAddress identity) {
        for (IProfile profile : accountHeader.getProfiles()) {
            if (profile instanceof ProfileDrawerItem) {
                ProfileDrawerItem profileDrawerItem = (ProfileDrawerItem) profile;
                if (identity.equals(profileDrawerItem.getTag())) {
                    profileDrawerItem
                        .withName(identity.toString())
                        .withTag(identity);
                    return;
                }
            }
        }
    }

    public void removeIdentityEntry(BitmessageAddress identity) {
        for (IProfile profile : accountHeader.getProfiles()) {
            if (profile instanceof ProfileDrawerItem) {
                ProfileDrawerItem profileDrawerItem = (ProfileDrawerItem) profile;
                if (identity.equals(profileDrawerItem.getTag())) {
                    accountHeader.removeProfile(profile);
                    return;
                }
            }
        }
    }

    private void checkAndStartNode() {
        if (Preferences.isConnectionAllowed(MainActivity.this)) {
            startService(new Intent(this, BitmessageService.class));
        } else {
            startActivity(new Intent(this, FullNodeDialogActivity.class));
        }
    }

    @Override
    public void updateUnread() {
        for (IDrawerItem item : drawer.getDrawerItems()) {
            if (item.getTag() instanceof Label) {
                Label label = (Label) item.getTag();
                if (label != LABEL_ARCHIVE) {
                    int unread = bmc.messages().countUnread(label);
                    if (unread > 0) {
                        ((PrimaryDrawerItem) item).withBadge(String.valueOf(unread));
                    } else {
                        ((PrimaryDrawerItem) item).withBadge((String) null);
                    }
                    drawer.updateItem(item);
                }
            }
        }
    }

    public static void updateNodeSwitch() {
        final MainActivity i = getInstance();
        if (i != null) {
            i.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    i.nodeSwitch.withChecked(Preferences.isFullNodeActive(i));
                    i.drawer.updateStickyFooterItem(i.nodeSwitch);
                }
            });
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
            if (item instanceof Plaintext) {
                fragment = new MessageDetailFragment();
            } else if (item instanceof String) {
                fragment = new AddressDetailFragment();
            } else {
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but was " + item.getClass().getSimpleName());
            }
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.message_detail_container, fragment)
                .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent;
            if (item instanceof Plaintext) {
                detailIntent = new Intent(this, MessageDetailActivity.class);
                detailIntent.putExtra(EXTRA_SHOW_LABEL, selectedLabel);
            } else if (item instanceof String) {
                detailIntent = new Intent(this, AddressDetailActivity.class);
            } else {
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but " +
                    "was "
                    + item.getClass().getSimpleName());
            }
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

    public static MainActivity getInstance() {
        if (instance == null) return null;
        return instance.get();
    }
}
