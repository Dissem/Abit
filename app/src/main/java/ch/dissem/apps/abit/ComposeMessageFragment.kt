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

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_BROADCAST
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_CONTENT
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_ENCODING
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_IDENTITY
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_PARENT
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_RECIPIENT
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_SUBJECT
import ch.dissem.apps.abit.adapter.ContactAdapter
import ch.dissem.apps.abit.dialog.SelectEncodingDialogFragment
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Preferences
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.Plaintext.Type.BROADCAST
import ch.dissem.bitmessage.entity.Plaintext.Type.MSG
import ch.dissem.bitmessage.entity.valueobject.extended.Message
import kotlinx.android.synthetic.main.fragment_compose_message.*

/**
 * Compose a new message.
 */
class ComposeMessageFragment : Fragment() {
    private lateinit var identity: BitmessageAddress
    private var recipient: BitmessageAddress? = null
    private var subject: String = ""
    private var content: String = ""

    private var broadcast: Boolean = false
    private var encoding: Plaintext.Encoding = Plaintext.Encoding.SIMPLE
    private var parent: Plaintext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { arguments ->
            var id = arguments.getSerializable(EXTRA_IDENTITY) as? BitmessageAddress
            if (context != null && (id == null || id.privateKey == null)) {
                id = Singleton.getIdentity(context)
            }
            if (id?.privateKey != null) {
                identity = id
            } else {
                throw IllegalStateException("No identity set for ComposeMessageFragment")
            }
            broadcast = arguments.getBoolean(EXTRA_BROADCAST, false)
            if (arguments.containsKey(EXTRA_RECIPIENT)) {
                recipient = arguments.getSerializable(EXTRA_RECIPIENT) as BitmessageAddress
            }
            if (arguments.containsKey(EXTRA_SUBJECT)) {
                subject = arguments.getString(EXTRA_SUBJECT)
            }
            if (arguments.containsKey(EXTRA_CONTENT)) {
                content = arguments.getString(EXTRA_CONTENT)
            }
            encoding = arguments.getSerializable(EXTRA_ENCODING) as? Plaintext.Encoding ?: Plaintext.Encoding.SIMPLE

            if (arguments.containsKey(EXTRA_PARENT)) {
                parent = arguments.getSerializable(EXTRA_PARENT) as Plaintext
            }
        } ?: {
            throw IllegalStateException("No identity set for ComposeMessageFragment")
        }.invoke()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_compose_message, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (broadcast) {
            recipient_input.visibility = View.GONE
        } else {
            val adapter = ContactAdapter(context)
            recipient_input.setAdapter(adapter)
            recipient_input.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> adapter.getItem(pos) }
            recipient_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    recipient = adapter.getItem(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // leave current selection
                }
            }
            recipient?.let { recipient_input.setText(it.toString()) }
        }
        subject_input.setText(subject)
        body_input.setText(content)

        when {
            recipient == null -> recipient_input.requestFocus()
            subject.isEmpty() -> subject_input.requestFocus()
            else -> {
                body_input.requestFocus()
                body_input.setSelection(0)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.compose, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send -> {
                send()
                return true
            }
            R.id.select_encoding -> {
                val encodingDialog = SelectEncodingDialogFragment()
                val args = Bundle()
                args.putSerializable(EXTRA_ENCODING, encoding)
                encodingDialog.arguments = args
                encodingDialog.setTargetFragment(this, 0)
                encodingDialog.show(fragmentManager, "select encoding dialog")
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && data != null && resultCode == RESULT_OK) {
            encoding = data.getSerializableExtra(EXTRA_ENCODING) as Plaintext.Encoding
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun send() {
        val builder: Plaintext.Builder
        val bmc = Singleton.getBitmessageContext(context)
        if (broadcast) {
            builder = Plaintext.Builder(BROADCAST).from(identity)
        } else {
            val inputString = recipient_input.text.toString()
            if (recipient == null || recipient?.toString() != inputString) {
                try {
                    recipient = BitmessageAddress(inputString)
                } catch (e: Exception) {
                    val contacts = Singleton.getAddressRepository(context).getContacts()
                    for (contact in contacts) {
                        if (inputString.equals(contact.alias, ignoreCase = true)) {
                            recipient = contact
                            if (inputString == contact.alias)
                                break
                        }
                    }
                }

            }
            if (recipient == null) {
                Toast.makeText(context, R.string.error_msg_recipient_missing, Toast.LENGTH_LONG).show()
                return
            }
            builder = Plaintext.Builder(MSG)
                    .from(identity)
                    .to(recipient)
        }
        if (!Preferences.requestAcknowledgements(context)) {
            builder.preventAck()
        }
        when (encoding) {
            Plaintext.Encoding.SIMPLE -> builder.message(
                    subject_input.text.toString(),
                    body_input.text.toString()
            )
            Plaintext.Encoding.EXTENDED -> builder.message(
                    Message.Builder()
                            .subject(subject_input.text.toString())
                            .body(body_input.text.toString())
                            .addParent(parent)
                            .build()
            )
            else -> {
                Toast.makeText(
                        context,
                        context.getString(R.string.error_unsupported_encoding, encoding),
                        Toast.LENGTH_LONG
                ).show()
                builder.message(
                        subject_input.text.toString(),
                        body_input.text.toString()
                )
            }
        }
        bmc.send(builder.build())
        activity.finish()
    }
}

