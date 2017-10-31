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

package ch.dissem.apps.abit.dialog

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import ch.dissem.apps.abit.ImportIdentityActivity
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.payload.Pubkey
import kotlinx.android.synthetic.main.dialog_add_identity.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.uiThread

/**
 * @author Christian Basler
 */

class AddIdentityDialogFragment : AppCompatDialogFragment() {
    private lateinit var bmc: BitmessageContext
    private var parent: ViewGroup? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        bmc = Singleton.getBitmessageContext(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.setTitle(R.string.add_identity)
        parent = container
        return inflater.inflate(R.layout.dialog_add_identity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ok.setOnClickListener(View.OnClickListener {
            val ctx = activity?.baseContext ?: throw IllegalStateException("No context available")

            when (radioGroup.checkedRadioButtonId) {
                R.id.create_identity -> {
                    Toast.makeText(ctx,
                            R.string.toast_long_running_operation,
                            Toast.LENGTH_SHORT).show()
                    doAsync {
                        val identity = bmc.createIdentity(false, Pubkey.Feature.DOES_ACK)
                        uiThread {
                            Toast.makeText(ctx,
                                    R.string.toast_identity_created,
                                    Toast.LENGTH_SHORT).show()
                            val mainActivity = MainActivity.getInstance()
                            mainActivity?.addIdentityEntry(identity)
                        }
                    }
                }
                R.id.import_identity -> startActivity<ImportIdentityActivity>()
                R.id.add_chan -> addChanDialog()
                R.id.add_deterministic_address -> DeterministicIdentityDialogFragment().show(fragmentManager, "dialog")
                else -> return@OnClickListener
            }
            dismiss()
        })
        dismiss.setOnClickListener { dismiss() }
    }

    private fun addChanDialog() {
        val activity = activity ?: throw IllegalStateException("No activity available")
        val ctx = activity.baseContext ?: throw IllegalStateException("No context available")
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_input_passphrase, parent)
        AlertDialog.Builder(activity)
                .setTitle(R.string.add_chan)
                .setView(dialogView)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val passphrase = dialogView.findViewById<TextView>(R.id.passphrase)
                    Toast.makeText(ctx, R.string.toast_long_running_operation,
                            Toast.LENGTH_SHORT).show()
                    val pass = passphrase.text.toString()
                    doAsync {
                        val chan = bmc.createChan(pass)
                        chan.alias = pass
                        bmc.addresses.save(chan)
                        uiThread {
                            Toast.makeText(ctx,
                                    R.string.toast_chan_created,
                                    Toast.LENGTH_SHORT).show()
                            MainActivity.getInstance()?.addIdentityEntry(chan)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun getTheme() = R.style.FixedDialog
}
