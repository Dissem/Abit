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

package ch.dissem.apps.abit.dialog;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;

/**
 * @author Christian Basler
 */
public class DeterministicIdentityDialogFragment extends AppCompatDialogFragment {
    private BitmessageContext bmc;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        bmc = Singleton.getBitmessageContext(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
        savedInstanceState) {
        getDialog().setTitle(R.string.add_deterministic_address);
        View view = inflater.inflate(R.layout.dialog_add_deterministic_identity, container, false);
        view.findViewById(R.id.ok)
            .setOnClickListener(v -> {
                dismiss();
                final Context context = getActivity().getBaseContext();
                View dialogView = getView();
                TextView label = (TextView) dialogView.findViewById(R.id.label);
                TextView passphrase = (TextView) dialogView.findViewById(R.id.passphrase);
                TextView numberOfAddresses = (TextView) dialogView.findViewById(R.id
                    .number_of_identities);
                Switch shorter = (Switch) dialogView.findViewById(R.id.shorter);

                Toast.makeText(context, R.string.toast_long_running_operation,
                    Toast.LENGTH_SHORT).show();
                new AsyncTask<Object, Void, List<BitmessageAddress>>() {
                    @Override
                    protected List<BitmessageAddress> doInBackground(Object... args) {
                        String label = (String) args[0];
                        String pass = (String) args[1];
                        int numberOfAddresses = (int) args[2];
                        boolean shorter = (boolean) args[3];
                        List<BitmessageAddress> identities = bmc.createDeterministicAddresses
                            (pass,
                                numberOfAddresses, Pubkey.LATEST_VERSION, 1L, shorter);
                        int i = 0;
                        for (BitmessageAddress identity : identities) {
                            i++;
                            if (identities.size() == 1) {
                                identity.setAlias(label);
                            } else {
                                identity.setAlias(label + " (" + i + ")");
                            }
                            bmc.addresses().save(identity);
                        }
                        return identities;
                    }

                    @Override
                    protected void onPostExecute(List<BitmessageAddress> identities) {
                        int messageRes;
                        if (identities.size() == 1) {
                            messageRes = R.string.toast_identity_created;
                        } else {
                            messageRes = R.string.toast_identities_created;
                        }
                        Toast.makeText(context,
                            messageRes,
                            Toast.LENGTH_SHORT).show();
                        MainActivity mainActivity = MainActivity.getInstance();
                        if (mainActivity != null) {
                            for (BitmessageAddress identity : identities) {
                                mainActivity.addIdentityEntry(identity);
                            }
                        }
                    }
                }.execute(
                    label.getText().toString(),
                    passphrase.getText().toString(),
                    Integer.valueOf(numberOfAddresses.getText().toString()),
                    shorter.isChecked()
                );
            });
        view.findViewById(R.id.dismiss)
            .setOnClickListener(v -> dismiss());
        return view;
    }

    @Override
    public int getTheme() {
        return R.style.FixedDialog;
    }
}
