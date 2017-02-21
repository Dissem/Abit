package ch.dissem.apps.abit;

import android.os.Bundle;

/**
 * @author Christian Basler
 */
public class SettingsActivity extends DetailActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }
}
