package ch.dissem.apps.abit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import java.util.ArrayList;


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
 * {@link MessageListFragment.Callbacks} interface
 * to listen for item selections.
 * </p>
 */
public class MessageListActivity extends AppCompatActivity
        implements MessageListFragment.Callbacks {
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.message_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((MessageListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.message_list))
                    .setActivateOnItemClick(true);
        }

        createDrawer(toolbar);

        // TODO: If exposing deep links into your app, handle intents here.
    }

    private void createDrawer(Toolbar toolbar) {
        final ArrayList<IProfile> profiles = new ArrayList<>();
        for (BitmessageAddress identity : bmc.addresses().getIdentities()) {
            profiles.add(new ProfileDrawerItem()
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
                        .withName("Manage Account")
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
            drawerItems.add(new PrimaryDrawerItem().withName(label.toString()).withTag(label));
        }

        new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .withDrawerItems(drawerItems)
                .addStickyDrawerItems(
                        new SecondaryDrawerItem()
                                .withName(R.string.settings)
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem item) {
                        if (item.getTag() instanceof Label) {
                            selectedLabel = (Label) item.getTag();
                        } else if (item instanceof Nameable<?>) {
                            Nameable<?> ni = (Nameable<?>) item;
                            switch (ni.getNameRes()) {
                                case R.string.settings:
                                    startActivity(new Intent(MessageListActivity.this, SettingsActivity.class));
                                    break;
                            }
                        }
                        return false;
                    }
                })
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        boolean running = bmc.isRunning();
        menu.findItem(R.id.sync_enabled).setVisible(running);
        menu.findItem(R.id.sync_disabled).setVisible(!running);
        return true;
    }

    /**
     * Callback method from {@link MessageListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Plaintext plaintext) {
        if (twoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putSerializable(MessageDetailFragment.ARG_ITEM, plaintext);
            MessageDetailFragment fragment = new MessageDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.message_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, MessageDetailActivity.class);
            detailIntent.putExtra(MessageDetailFragment.ARG_ITEM, plaintext);
            startActivity(detailIntent);
        }
    }

    public Label getSelectedLabel() {
        return selectedLabel;
    }
}
