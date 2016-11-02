package ch.dissem.apps.abit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import ch.dissem.bitmessage.entity.valueobject.Label;


/**
 * An activity representing a single Message detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link MainActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link MessageDetailFragment}.
 */
public class MessageDetailActivity extends DetailActivity {
    private Label label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            label = (Label) getIntent().getSerializableExtra(MainActivity.EXTRA_SHOW_LABEL);
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putSerializable(MessageDetailFragment.ARG_ITEM,
                getIntent().getSerializableExtra(MessageDetailFragment.ARG_ITEM));
            MessageDetailFragment fragment = new MessageDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.content, fragment)
                .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent parentIntent = new Intent(this, MainActivity.class);
                parentIntent.putExtra(MainActivity.EXTRA_SHOW_LABEL, label);
                NavUtils.navigateUpTo(this, parentIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
