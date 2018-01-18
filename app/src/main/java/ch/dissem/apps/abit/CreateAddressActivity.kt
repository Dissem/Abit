/*
 * Copyright 2015 Christian Basler
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
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Base64.URL_SAFE
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.V2Pubkey
import ch.dissem.bitmessage.entity.payload.V3Pubkey
import ch.dissem.bitmessage.entity.payload.V4Pubkey
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

class CreateAddressActivity : AppCompatActivity() {
    private var pubkeyBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri != null)
            setContentView(R.layout.activity_open_bitmessage_link)
        else
            setContentView(R.layout.activity_create_bitmessage_address)

        val address = findViewById<TextView>(R.id.address)
        val label = findViewById<EditText>(R.id.label)
        val subscribe = findViewById<Switch>(R.id.subscribe)

        if (uri != null) {
            val addressText = getAddress(uri)
            val parameters = getParameters(uri)
            for (parameter in parameters) {
                val matcher = KEY_VALUE_PATTERN.matcher(parameter)
                if (matcher.find()) {
                    val key = matcher.group(1).toLowerCase()
                    val value = matcher.group(2)
                    when (key) {
                        "label" -> label.setText(value.trim { it <= ' ' })
                        "action" -> subscribe.isChecked = value.trim { it <= ' ' }.equals("subscribe", ignoreCase = true)
                        "pubkey" -> pubkeyBytes = Base64.decode(value, URL_SAFE)
                        else -> LOG.debug("Unknown attribute: $key=$value")
                    }
                }
            }

            address.text = addressText
        }

        val cancel = findViewById<Button>(R.id.cancel)
        cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        findViewById<Button>(R.id.do_import).setOnClickListener { onOK(address, label, subscribe) }
    }


    private fun onOK(address: TextView, label: EditText, subscribe: Switch) {
        val addressText = address.text.toString().trim { it <= ' ' }
        try {
            val bmAddress = BitmessageAddress(addressText)
            bmAddress.alias = label.text.toString()

            val bmc = Singleton.getBitmessageContext(applicationContext)
            bmc.addContact(bmAddress)
            if (subscribe.isChecked) {
                bmc.addSubscribtion(bmAddress)
            }
            pubkeyBytes?.let { pubkeyBytes ->
                try {
                    val pubkeyStream = ByteArrayInputStream(pubkeyBytes)
                    val stream = bmAddress.stream
                    when (bmAddress.version.toInt()) {
                        2 -> V2Pubkey.read(pubkeyStream, stream)
                        3 -> V3Pubkey.read(pubkeyStream, stream)
                        4 -> V4Pubkey(V3Pubkey.read(pubkeyStream, stream))
                        else -> null
                    }?.let { bmAddress.pubkey = it }
                } catch (ignore: Exception) {
                }
            }

            setResult(Activity.RESULT_OK)
            finish()
        } catch (e: RuntimeException) {
            address.error = getString(R.string.error_illegal_address)
        }
    }

    private fun getAddress(uri: Uri): String {
        val result = StringBuilder()
        val schemeSpecificPart = uri.schemeSpecificPart
        if (!schemeSpecificPart.startsWith("BM-")) {
            result.append("BM-")
        }
        when {
            schemeSpecificPart.contains("?") -> result.append(schemeSpecificPart.substring(0, schemeSpecificPart.indexOf('?')))
            schemeSpecificPart.contains("#") -> result.append(schemeSpecificPart.substring(0, schemeSpecificPart.indexOf('#')))
            else -> result.append(schemeSpecificPart)
        }
        return result.toString()
    }

    private fun getParameters(uri: Uri): Array<String> {
        val index = uri.schemeSpecificPart.indexOf('?')
        return if (index >= 0) {
            uri.schemeSpecificPart
                    .substring(index + 1)
                    .split("&".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
        } else {
            emptyArray()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CreateAddressActivity::class.java)

        private val KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z]+)=(.*)$")
    }
}
