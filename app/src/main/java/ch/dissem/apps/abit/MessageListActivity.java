package ch.dissem.apps.abit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import ch.dissem.apps.abit.listener.ActionBarListener;
import ch.dissem.apps.abit.listener.ListSelectionListener;
import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ch.dissem.apps.abit.listeners.ActionBarListener;
import ch.dissem.apps.abit.listeners.ListSelectionListener;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.synchronization.Authenticator;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import static ch.dissem.apps.abit.synchronization.StubProvider.AUTHORITY;


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
public class MessageListActivity extends AppCompatActivity
        implements ListSelectionListener<Serializable>, ActionBarListener {
    public static final String EXTRA_SHOW_MESSAGE = "ch.dissem.abit.ShowMessage";
    public static final String ACTION_SHOW_INBOX = "ch.dissem.abit.ShowInbox";

    private static final Logger LOG = LoggerFactory.getLogger(MessageListActivity.class);
    private static final long SYNC_FREQUENCY = 15 * 60; // seconds
    private static final int ADD_IDENTITY = 1;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean twoPane;

    private AccountHeader accountHeader;
    private BitmessageContext bmc;
    private Label selectedLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bmc = Singleton.getBitmessageContext(this);
        selectedLabel = bmc.messages().getLabels().get(0);

        setContentView(R.layout.activity_message_list);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MessageListFragment listFragment = new MessageListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.item_list, listFragment).commit();

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

        Singleton.getMessageListener(this).resetNotification();

        // handle intents
        if (getIntent().hasExtra(EXTRA_SHOW_MESSAGE)) {
            onItemSelected(getIntent().getSerializableExtra(EXTRA_SHOW_MESSAGE));
        }

        createSyncAccount();
    }

    private void createSyncAccount() {
        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = new Account(Authenticator.ACCOUNT_NAME, Authenticator.ACCOUNT_TYPE);

        if (AccountManager.get(this).addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);
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

    private void createDrawer(Toolbar toolbar) {
        final ArrayList<IProfile> profiles = new ArrayList<>();
        for (BitmessageAddress identity : bmc.addresses().getIdentities()) {
            LOG.info("Adding identity " + identity.getAddress());
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
        );
        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withProfiles(profiles)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile.getIdentifier() == ADD_IDENTITY) {
                            BitmessageAddress identity = bmc.createIdentity(false);
                            IProfile newProfile = new ProfileDrawerItem()
                                    .withName(identity.toString())
                                    .withEmail(identity.getAddress())
                                    .withTag(identity);
                            if (accountHeader.getProfiles() != null) {
                                //we know that there are 2 setting elements. set the new profile above them ;)
                                accountHeader.addProfile(newProfile, accountHeader.getProfiles().size() - 2);
                            } else {
                                accountHeader.addProfiles(newProfile);
                            }
                        }
                        // false if it should close the drawer
                        return false;
                    }
                })
                .build();

        ArrayList<IDrawerItem> drawerItems = new ArrayList<>();
        for (Label label : bmc.messages().getLabels()) {
            PrimaryDrawerItem item = new PrimaryDrawerItem().withName(label.toString()).withTag(label);
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
            drawerItems.add(item);
        }
        drawerItems.add(new PrimaryDrawerItem()
                        .withName(R.string.archive)
                        .withTag(null)
                        .withIcon(CommunityMaterial.Icon.cmd_archive)
        );


        new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .withDrawerItems(drawerItems)
                .addStickyDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.subscriptions)
                                .withIcon(CommunityMaterial.Icon.cmd_rss_box),
                        new PrimaryDrawerItem()
                                .withName(R.string.settings)
                                .withIcon(GoogleMaterial.Icon.gmd_settings),
                        new SwitchDrawerItem()
                                .withName(R.string.full_node)
                                .withIcon(CommunityMaterial.Icon.cmd_cloud_outline)
                                .withOnCheckedChangeListener(new OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                                        // TODO: warn user, option to restrict to WiFi
                                        if (isChecked && !bmc.isRunning()) {
                                            bmc.startup();
                                            new NetworkNotification(MessageListActivity.this).show();
                                        } else if (bmc.isRunning()) bmc.shutdown();
                                    }
                                })
                                .withChecked(bmc.isRunning())
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem item) {
                        if (item.getTag() instanceof Label) {
                            selectedLabel = (Label) item.getTag();
                            if (!(getSupportFragmentManager().findFragmentById(R.id.item_list) instanceof MessageListFragment)) {
                                MessageListFragment listFragment = new MessageListFragment(getApplicationContext());
                                changeList(listFragment);
                                listFragment.updateList(selectedLabel);
                            } else {
                                ((MessageListFragment) getSupportFragmentManager()
                                        .findFragmentById(R.id.item_list)).updateList(selectedLabel);
                            }
                            return false;
                        } else if (item instanceof Nameable<?>) {
                            Nameable<?> ni = (Nameable<?>) item;
                            switch (ni.getNameRes()) {
                                case R.string.subscriptions:
                                    if (!(getSupportFragmentManager().findFragmentById(R.id.item_list) instanceof SubscriptionListFragment)) {
                                        changeList(new SubscriptionListFragment());
                                    } else {
                                        ((SubscriptionListFragment) getSupportFragmentManager()
                                                .findFragmentById(R.id.item_list)).updateList();
                                    }
                                    break;
                                case R.string.settings:
                                    startActivity(new Intent(MessageListActivity.this, SettingsActivity.class));
                                    break;
                                case R.string.full_node:
                                    return true;
                            }
                        }
                        return false;
                    }
                })
                .withCloseOnClick(true)
                .build();
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
                fragment = new SubscriptionDetailFragment();
            else
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but was "
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
                detailIntent = new Intent(this, SubscriptionDetailActivity.class);
            else
                throw new IllegalArgumentException("Plaintext or BitmessageAddress expected, but was "
                        + item.getClass().getSimpleName());

            detailIntent.putExtra(MessageDetailFragment.ARG_ITEM, item);
            startActivity(detailIntent);
        }
    }

    @Override
    public void updateTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    public Label getSelectedLabel() {
        return selectedLabel;
    }

}
