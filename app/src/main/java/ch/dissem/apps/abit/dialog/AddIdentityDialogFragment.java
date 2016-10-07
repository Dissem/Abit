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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import ch.dissem.apps.abit.ImportIdentityActivity;
import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;

/**
 * @author Christian Basler
 */

public class AddIdentityDialogFragment extends AppCompatDialogFragment {
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
        getDialog().setTitle(R.string.add_identity);
        View view = inflater.inflate(R.layout.dialog_add_identity, container, false);
        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        view.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context ctx = getActivity().getBaseContext();
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.create_identity:
                        Toast.makeText(ctx,
                            R.string.toast_long_running_operation,
                            Toast.LENGTH_SHORT).show();
                        new AsyncTask<Void, Void, BitmessageAddress>() {
                            @Override
                            protected BitmessageAddress doInBackground(Void... args) {
                                return bmc.createIdentity(false, Pubkey.Feature.DOES_ACK);
                            }

                            @Override
                            protected void onPostExecute(BitmessageAddress chan) {
                                Toast.makeText(ctx,
                                    R.string.toast_identity_created,
                                    Toast.LENGTH_SHORT).show();
                                MainActivity mainActivity = MainActivity.getInstance();
                                if (mainActivity != null) {
                                    mainActivity.addIdentityEntry(chan);
                                }
                            }
                        }.execute();
                        break;
                    case R.id.import_identity:
                        startActivity(new Intent(ctx, ImportIdentityActivity.class));
                        break;
                    case R.id.add_chan:
                        addChanDialog();
                        break;
                    case R.id.add_deterministic_address:
                        new DeterministicIdentityDialogFragment().show(getFragmentManager(),
                            "dialog");
                        break;
                    default:
                        return;
                }
                dismiss();
            }
        });
        view.findViewById(R.id.dismiss)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        return view;
    }

    private void addChanDialog() {
        FragmentActivity activity = getActivity();
        final Context ctx = activity.getBaseContext();
        @SuppressLint("InflateParams")
        final View dialogView = activity.getLayoutInflater()
            .inflate(R.layout.dialog_input_passphrase, null);
        new AlertDialog.Builder(activity)
            .setTitle(R.string.add_chan)
            .setView(dialogView)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    TextView passphrase = (TextView) dialogView.findViewById(R.id.passphrase);
                    Toast.makeText(ctx, R.string.toast_long_running_operation,
                        Toast.LENGTH_SHORT).show();
                    new AsyncTask<String, Void, BitmessageAddress>() {
                        @Override
                        protected BitmessageAddress doInBackground(String... args) {
                            String pass = args[0];
                            BitmessageAddress chan = bmc.createChan(pass);
                            chan.setAlias(pass);
                            bmc.addresses().save(chan);
                            return chan;
                        }

                        @Override
                        protected void onPostExecute(BitmessageAddress chan) {
                            Toast.makeText(ctx,
                                R.string.toast_chan_created,
                                Toast.LENGTH_SHORT).show();
                            MainActivity mainActivity = MainActivity.getInstance();
                            if (mainActivity != null) {
                                mainActivity.addIdentityEntry(chan);
                            }
                        }
                    }.execute(passphrase.getText().toString());
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public int getTheme() {
        return R.style.FixedDialog;
    }
}
