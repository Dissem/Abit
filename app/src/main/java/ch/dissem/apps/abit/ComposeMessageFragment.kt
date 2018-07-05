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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_BROADCAST
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_CONTENT
import ch.dissem.apps.abit.ComposeMessageActivity.Companion.EXTRA_DRAFT
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
import ch.dissem.bitmessage.entity.valueobject.ExtendedEncoding
import ch.dissem.bitmessage.entity.valueobject.InventoryVector
import ch.dissem.bitmessage.entity.valueobject.Label
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
    private val parents = mutableListOf<InventoryVector>()

    private var draft: Plaintext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.apply {
            val draft = getSerializable(EXTRA_DRAFT) as Plaintext?
            if (draft != null) {
                this@ComposeMessageFragment.draft = draft
                identity = draft.from
                recipient = draft.to
                subject = draft.subject ?: ""
                content = draft.text ?: ""
                encoding = draft.encoding ?: Plaintext.Encoding.SIMPLE
                parents.addAll(draft.parents)
            } else {
                var id = getSerializable(EXTRA_IDENTITY) as? BitmessageAddress
                if (context != null && id?.privateKey == null) {
                    id = Singleton.getIdentity(context!!)
                }
                if (id?.privateKey != null) {
                    identity = id
                } else {
                    throw IllegalStateException("No identity set for ComposeMessageFragment")
                }
                broadcast = getBoolean(EXTRA_BROADCAST, false)
                if (containsKey(EXTRA_RECIPIENT)) {
                    recipient = getSerializable(EXTRA_RECIPIENT) as BitmessageAddress
                }
                if (containsKey(EXTRA_SUBJECT)) {
                    subject = getString(EXTRA_SUBJECT)
                }
                if (containsKey(EXTRA_CONTENT)) {
                    content = getString(EXTRA_CONTENT)
                }
                encoding = getSerializable(EXTRA_ENCODING) as? Plaintext.Encoding ?:
                    Plaintext.Encoding.SIMPLE

                if (containsKey(EXTRA_PARENT)) {
                    val parent = getSerializable(EXTRA_PARENT) as Plaintext
                    parent.inventoryVector?.let { parents.add(it) }
                }
            }
        } ?: throw IllegalStateException("No identity set for ComposeMessageFragment")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_compose_message, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { ctx ->
            val identities = Singleton.getAddressRepository(ctx).getIdentities()
            sender_input.adapter = ContactAdapter(ctx, identities, true)
            val index = identities.indexOf(Singleton.getIdentity(ctx))
            if (index >= 0) {
                sender_input.setSelection(index)
            }

            if (broadcast) {
                recipient_input.visibility = View.GONE
            } else {
                val adapter = ContactAdapter(
                    ctx,
                    Singleton.getAddressRepository(ctx).getContacts()
                )
                recipient_input.setAdapter(adapter)
                recipient_input.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, pos, _ -> recipient = adapter.getItem(pos) }

                recipient_input.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                        ) {
                            recipient = adapter.getItem(position)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) =
                            Unit // leave current selection
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        if (requestCode == 0 && data != null && resultCode == RESULT_OK) {
            encoding = data.getSerializableExtra(EXTRA_ENCODING) as Plaintext.Encoding
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    private fun build(ctx: Context): Plaintext {
        val builder: Plaintext.Builder
        if (broadcast) {
            builder = Plaintext.Builder(BROADCAST)
        } else {
            val inputString = recipient_input.text.toString()
            if (recipient == null || recipient?.toString() != inputString) {
                try {
                    recipient = BitmessageAddress(inputString)
                } catch (e: Exception) {
                    val contacts = Singleton.getAddressRepository(ctx).getContacts()
                    for (contact in contacts) {
                        if (inputString.equals(contact.alias, ignoreCase = true)) {
                            recipient = contact
                            if (inputString == contact.alias)
                                break
                        }
                    }
                }

            }
            builder = Plaintext.Builder(MSG)
                .to(recipient)
        }
        val sender = sender_input.selectedItem as? ch.dissem.bitmessage.entity.BitmessageAddress
        sender?.let { builder.from(it) }
        if (!Preferences.requestAcknowledgements(ctx)) {
            builder.preventAck()
        }
        when (encoding) {
            Plaintext.Encoding.SIMPLE -> builder.message(
                subject_input.text.toString(),
                body_input.text.toString()
            )
            Plaintext.Encoding.EXTENDED -> builder.message(
                ExtendedEncoding(
                    Message(
                        subject = subject_input.text.toString(),
                        body = body_input.text.toString(),
                        parents = parents,
                        files = emptyList()
                    )
                )
            )
            else -> {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.error_unsupported_encoding, encoding),
                    Toast.LENGTH_LONG
                ).show()
                builder.message(
                    subject_input.text.toString(),
                    body_input.text.toString()
                )
            }
        }
        draft?.id?.let { builder.id(it) }
        return builder.build()
    }

    override fun onPause() {
        if (draft?.labels?.any { it.type == Label.Type.DRAFT } != false) {
            context?.let { ctx ->
                draft = build(ctx).also { msg ->
                    Singleton.labeler.markAsDraft(msg)
                    Singleton.getMessageRepository(ctx).save(msg)
                }
                Toast.makeText(ctx, "Message saved as draft", Toast.LENGTH_LONG).show()
            } ?: throw IllegalStateException("Context is not available")
        }
        super.onPause()
    }

    override fun onDestroyView() {
        identity = sender_input.selectedItem as BitmessageAddress
        // recipient is set when one is selected
        subject = subject_input.text?.toString() ?: ""
        content = body_input.text?.toString() ?: ""
        super.onDestroyView()
    }

    private fun send() {
        val ctx = activity ?: throw IllegalStateException("Fragment is not attached to an activity")
        if (recipient == null) {
            Toast.makeText(ctx, R.string.error_msg_recipient_missing, Toast.LENGTH_LONG)
                .show()
            return
        }
        build(ctx).let { message ->
            draft = message
            Singleton.getBitmessageContext(ctx).send(message)
        }
        ctx.finish()
    }
}

