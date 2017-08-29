package ch.dissem.apps.abit

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.view.MenuItem

import ch.dissem.bitmessage.entity.valueobject.Label


/**
 * An activity representing a single Message detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [MainActivity].
 *
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a [MessageDetailFragment].
 */
class MessageDetailActivity : DetailActivity() {
    private var label: Label? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            label = intent.getSerializableExtra(MainActivity.EXTRA_SHOW_LABEL) as Label?
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val arguments = Bundle()
            arguments.putSerializable(MessageDetailFragment.ARG_ITEM,
                    intent.getSerializableExtra(MessageDetailFragment.ARG_ITEM))
            val fragment = MessageDetailFragment()
            fragment.arguments = arguments
            supportFragmentManager.beginTransaction()
                    .add(R.id.content, fragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            val parentIntent = Intent(this, MainActivity::class.java)
            parentIntent.putExtra(MainActivity.EXTRA_SHOW_LABEL, label)
            NavUtils.navigateUpTo(this, parentIntent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
