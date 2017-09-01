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

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.payload.Pubkey
import kotlinx.android.synthetic.main.dialog_add_deterministic_identity.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * @author Christian Basler
 */
class DeterministicIdentityDialogFragment : AppCompatDialogFragment() {
    private lateinit var bmc: BitmessageContext

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        bmc = Singleton.getBitmessageContext(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.setTitle(R.string.add_deterministic_address)
        return inflater.inflate(R.layout.dialog_add_deterministic_identity, container, false)
    }

    override fun onViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(dialogView, savedInstanceState)
        ok.setOnClickListener {
            dismiss()
            val context = activity.baseContext
            val passphraseText = passphrase.text.toString()

            Toast.makeText(context, R.string.toast_long_running_operation, Toast.LENGTH_SHORT).show()
            doAsync {
                val identities = bmc.createDeterministicAddresses(
                        passphraseText,
                        number_of_identities.text.toString().toInt(),
                        Pubkey.LATEST_VERSION,
                        1L,
                        shorter.isChecked
                )
                for ((i, identity) in identities.withIndex()) {
                    if (identities.size == 1) {
                        identity.alias = label.text.toString()
                    } else {
                        identity.alias = "${label.text} (${i + 1})"
                    }
                    bmc.addresses.save(identity)
                }
                uiThread {
                    val messageRes = if (identities.size == 1) {
                        R.string.toast_identity_created
                    } else {
                        R.string.toast_identities_created
                    }
                    Toast.makeText(context,
                            messageRes,
                            Toast.LENGTH_SHORT).show()
                    MainActivity.getInstance()?.let { mainActivity ->
                        identities.forEach { identity ->
                            mainActivity.addIdentityEntry(identity)
                        }
                    }
                }
            }
        }
        dismiss.setOnClickListener { dismiss() }
    }

    override fun getTheme() = R.style.FixedDialog
}
