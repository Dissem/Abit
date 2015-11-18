package ch.dissem.apps.abit;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.List;

import ch.dissem.apps.abit.adapter.ContactAdapter;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.entity.BitmessageAddress;

import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_IDENTITY;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_RECIPIENT;

/**
 * Compose a new message.
 */
public class ComposeMessageFragment extends Fragment {
    private BitmessageAddress identity;
    private BitmessageAddress recipient;
    private AutoCompleteTextView recipientInput;
    private EditText subjectInput;
    private EditText bodyInput;

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
            if (getArguments().containsKey(EXTRA_RECIPIENT)) {
                recipient = (BitmessageAddress) getArguments().getSerializable(EXTRA_RECIPIENT);
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
        final ContactAdapter adapter = new ContactAdapter(getContext());
        recipientInput.setAdapter(adapter);
        recipientInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                recipient = adapter.getItem(position);
            }
        });
        recipientInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                recipient = adapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if (recipient != null) {
            recipientInput.setText(recipient.toString());
        }
        subjectInput = (EditText) rootView.findViewById(R.id.subject);
        bodyInput = (EditText) rootView.findViewById(R.id.body);
//        bodyInput.setInputType(EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
//        bodyInput.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
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
                String inputString = recipientInput.getText().toString();
                if (recipient == null || !recipient.toString().equals(inputString)) {
                    try {
                        recipient = new BitmessageAddress(inputString);
                    } catch (Exception e) {
                        List<BitmessageAddress> contacts = Singleton.getAddressRepository(getContext()).getContacts();
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
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

