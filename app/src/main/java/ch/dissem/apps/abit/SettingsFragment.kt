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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.widget.Toast
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.synchronization.SyncAdapter
import ch.dissem.apps.abit.util.Constants.PREFERENCE_SERVER_POW
import ch.dissem.apps.abit.util.Constants.PREFERENCE_TRUSTED_NODE
import ch.dissem.apps.abit.util.Exports
import ch.dissem.apps.abit.util.Preferences
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.uiThread

/**
 * @author Christian Basler
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference("about")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            (activity as? MainActivity)?.let { activity ->
                val libsBuilder = LibsBuilder()
                    .withActivityTitle(activity.getString(R.string.about))
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .withAboutDescription(getString(R.string.about_app))
                if (activity.hasDetailPane) {
                    activity.setDetailView(libsBuilder.supportFragment())
                } else {
                    libsBuilder.start(activity)
                }
            }
            return@OnPreferenceClickListener true
        }
        val cleanup = findPreference("cleanup")
        cleanup?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ctx = activity?.applicationContext ?: throw IllegalStateException("Context not available")
            cleanup.isEnabled = false
            Toast.makeText(ctx, R.string.cleanup_notification_start, Toast.LENGTH_SHORT).show()

            doAsync {
                val bmc = Singleton.getBitmessageContext(ctx)
                bmc.internals.nodeRegistry.clear()
                bmc.cleanup()
                Preferences.cleanupExportDirectory(ctx)

                uiThread {
                    Toast.makeText(
                        ctx,
                        R.string.cleanup_notification_end,
                        Toast.LENGTH_LONG
                    ).show()
                    cleanup.isEnabled = true
                }
            }
            return@OnPreferenceClickListener true
        }

        findPreference("export")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ctx = context ?: throw IllegalStateException("No context available")

            indeterminateProgressDialog(R.string.export_data_summary, R.string.export_data).apply {
                doAsync {
                    val exportDirectory = Preferences.getExportDirectory(ctx)
                    exportDirectory.mkdirs()
                    val file = Exports.exportData(exportDirectory, ctx)
                    val contentUri = getUriForFile(ctx, "ch.dissem.apps.abit.fileprovider", file)
                    val intent = Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "application/zip"
                    intent.putExtra(Intent.EXTRA_SUBJECT, "abit-export.zip")
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    startActivityForResult(Intent.createChooser(intent, ""), WRITE_EXPORT_REQUEST_CODE)
                    uiThread {
                        dismiss()
                    }
                }
            }
            return@OnPreferenceClickListener true
        }

        findPreference("import")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"

            startActivityForResult(intent, READ_IMPORT_REQUEST_CODE)
            return@OnPreferenceClickListener true
        }

        findPreference("status").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val activity = activity as MainActivity
            if (activity.hasDetailPane) {
                activity.setDetailView(StatusFragment())
            } else {
                startActivity<StatusActivity>()
            }
            return@OnPreferenceClickListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val ctx = context ?: throw IllegalStateException("No context available")
        when (requestCode) {
            WRITE_EXPORT_REQUEST_CODE -> Preferences.cleanupExportDirectory(ctx)
            READ_IMPORT_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    indeterminateProgressDialog(R.string.import_data_summary, R.string.import_data).apply {
                        doAsync {
                            Exports.importData(data.data, ctx)
                            uiThread {
                                dismiss()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttach(ctx: Context?) {
        super.onAttach(ctx)
        (ctx as? MainActivity)?.floatingActionButton?.hide()
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .registerOnSharedPreferenceChangeListener(this)

        (ctx as? MainActivity)?.updateTitle(getString(R.string.settings))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PREFERENCE_TRUSTED_NODE -> toggleSyncTrustedNode(sharedPreferences)
            PREFERENCE_SERVER_POW -> toggleSyncServerPOW(sharedPreferences)
        }
    }

    private fun toggleSyncTrustedNode(sharedPreferences: SharedPreferences) {
        val node = sharedPreferences.getString(PREFERENCE_TRUSTED_NODE, null)
        val ctx = context ?: throw IllegalStateException("No context available")
        if (node != null) {
            SyncAdapter.startSync(ctx)
        } else {
            SyncAdapter.stopSync(ctx)
        }
    }

    private fun toggleSyncServerPOW(sharedPreferences: SharedPreferences) {
        val node = sharedPreferences.getString(PREFERENCE_TRUSTED_NODE, null)
        if (node != null) {
            val ctx = context ?: throw IllegalStateException("No context available")
            if (sharedPreferences.getBoolean(PREFERENCE_SERVER_POW, false)) {
                SyncAdapter.startPowSync(ctx)
            } else {
                SyncAdapter.stopPowSync(ctx)
            }
        }
    }

    companion object {
        const val WRITE_EXPORT_REQUEST_CODE = 1
        const val READ_IMPORT_REQUEST_CODE = 2
    }
}
