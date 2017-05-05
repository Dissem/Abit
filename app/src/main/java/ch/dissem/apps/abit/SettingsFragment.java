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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import ch.dissem.apps.abit.repository.AndroidNodeRegistry;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.synchronization.SyncAdapter;
import ch.dissem.bitmessage.BitmessageContext;

import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SERVER_POW;
import static ch.dissem.apps.abit.util.Constants.PREFERENCE_TRUSTED_NODE;

/**
 * @author Christian Basler
 */
public class SettingsFragment
    extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference about = findPreference("about");
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new LibsBuilder()
                    .withActivityTitle(getActivity().getString(R.string.about))
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .withAboutDescription(getString(R.string.about_app))
                    .start(getActivity());
                return true;
            }
        });
        final Preference cleanup = findPreference("cleanup");
        cleanup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AsyncTask<Void, Void, Void>() {
                    private Context ctx = getActivity().getApplicationContext();

                    @Override
                    protected void onPreExecute() {
                        cleanup.setEnabled(false);
                        Toast.makeText(ctx, R.string.cleanup_notification_start, Toast
                            .LENGTH_SHORT).show();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        BitmessageContext bmc = Singleton.getBitmessageContext(ctx);
                        bmc.cleanup();
                        bmc.internals().getNodeRegistry().clear();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        Toast.makeText(
                            ctx,
                            R.string.cleanup_notification_end,
                            Toast.LENGTH_LONG
                        ).show();
                        cleanup.setEnabled(true);
                    }
                }.execute();
                return true;
            }
        });

        Preference status = findPreference("status");
        status.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), StatusActivity.class));
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREFERENCE_TRUSTED_NODE: {
                String node = sharedPreferences.getString(PREFERENCE_TRUSTED_NODE, null);
                if (node != null) {
                    SyncAdapter.startSync(getActivity());
                } else {
                    SyncAdapter.stopSync(getActivity());
                }
                break;
            }
            case PREFERENCE_SERVER_POW: {
                String node = sharedPreferences.getString(PREFERENCE_TRUSTED_NODE, null);
                if (node != null) {
                    if (sharedPreferences.getBoolean(PREFERENCE_SERVER_POW, false)) {
                        SyncAdapter.startPowSync(getActivity());
                    } else {
                        SyncAdapter.stopPowSync(getActivity());
                    }
                }
                break;
            }
        }
    }
}
