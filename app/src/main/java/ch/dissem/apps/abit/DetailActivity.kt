package ch.dissem.apps.abit

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.mikepenz.materialize.MaterializeBuilder
import kotlinx.android.synthetic.main.scrolling_toolbar_layout.*

/**
 * @author Christian Basler
 */
abstract class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scrolling_toolbar_layout)

        setSupportActionBar(toolbar)
        // Show the Up button in the action bar.
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        MaterializeBuilder()
                .withActivity(this)
                .withStatusBarColorRes(R.color.colorPrimaryDark)
                .withTranslucentStatusBarProgrammatically(true)
                .withStatusBarPadding(true)
                .build()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, Intent(this, MainActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
