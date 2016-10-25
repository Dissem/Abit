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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.List;

import ch.dissem.apps.abit.adapter.ContactAdapter;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;

import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_BROADCAST;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_CONTENT;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_IDENTITY;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_RECIPIENT;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_SUBJECT;

/**
 * Compose a new message.
 */
public class ComposeMessageFragment extends Fragment {
    private BitmessageAddress identity;
    private BitmessageAddress recipient;
    private String subject;
    private String content;
    private AutoCompleteTextView recipientInput;
    private EditText subjectInput;
    private EditText bodyInput;
    private boolean broadcast;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ComposeMessageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(EXTRA_IDENTITY)) {
                identity = (BitmessageAddress) getArguments().getSerializable(EXTRA_IDENTITY);
            } else {
                throw new RuntimeException("No identity set for ComposeMessageFragment");
            }
            broadcast = getArguments().getBoolean(EXTRA_BROADCAST, false);
            if (getArguments().containsKey(EXTRA_RECIPIENT)) {
                recipient = (BitmessageAddress) getArguments().getSerializable(EXTRA_RECIPIENT);
            }
            if (getArguments().containsKey(EXTRA_SUBJECT)) {
                subject = getArguments().getString(EXTRA_SUBJECT);
            }
            if (getArguments().containsKey(EXTRA_CONTENT)) {
                content = getArguments().getString(EXTRA_CONTENT);
            }
        } else {
            throw new RuntimeException("No identity set for ComposeMessageFragment");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_compose_message, container, false);
        recipientInput = (AutoCompleteTextView) rootView.findViewById(R.id.recipient);
        if (broadcast) {
            recipientInput.setVisibility(View.GONE);
        } else {
            final ContactAdapter adapter = new ContactAdapter(getContext());
            recipientInput.setAdapter(adapter);
            recipientInput.setOnItemClickListener(
                (parent, view, position, id) -> recipient = adapter.getItem(position)
            );
            recipientInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long
                    id) {
                    recipient = adapter.getItem(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            if (recipient != null) {
                recipientInput.setText(recipient.toString());
            }
        }
        subjectInput = (EditText) rootView.findViewById(R.id.subject);
        subjectInput.setText(subject);
        bodyInput = (EditText) rootView.findViewById(R.id.body);
        bodyInput.setText(content);

        if (recipient == null) {
            recipientInput.requestFocus();
        } else if (subject == null || subject.isEmpty()) {
            subjectInput.requestFocus();
        } else {
            bodyInput.requestFocus();
            bodyInput.setSelection(0);
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.compose, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.send:
                if (broadcast) {
                    Singleton.getBitmessageContext(getContext()).broadcast(identity,
                        subjectInput.getText().toString(),
                        bodyInput.getText().toString());
                } else {
                    String inputString = recipientInput.getText().toString();
                    if (recipient == null || !recipient.toString().equals(inputString)) {
                        try {
                            recipient = new BitmessageAddress(inputString);
                        } catch (Exception e) {
                            List<BitmessageAddress> contacts = Singleton.getAddressRepository
                                (getContext()).getContacts();
                            for (BitmessageAddress contact : contacts) {
                                if (inputString.equalsIgnoreCase(contact.getAlias())) {
                                    recipient = contact;
                                    if (inputString.equals(contact.getAlias()))
                                        break;
                                }
                            }
                        }
                    }
                    Singleton.getBitmessageContext(getContext()).send(identity, recipient,
                        subjectInput.getText().toString(),
                        bodyInput.getText().toString());
                }
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

