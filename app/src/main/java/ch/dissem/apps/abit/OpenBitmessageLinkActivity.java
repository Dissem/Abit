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

package ch.dissem.apps.abit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dissem.apps.abit.service.BitmessageService;
import ch.dissem.bitmessage.entity.BitmessageAddress;

import static ch.dissem.apps.abit.service.BitmessageService.DATA_FIELD_ADDRESS;
import static ch.dissem.apps.abit.service.BitmessageService.MSG_ADD_CONTACT;
import static ch.dissem.apps.abit.service.BitmessageService.MSG_SUBSCRIBE;
import static ch.dissem.apps.abit.service.BitmessageService.MSG_SUBSCRIBE_AND_ADD_CONTACT;

public class OpenBitmessageLinkActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(OpenBitmessageLinkActivity.class);

    private Messenger service;
    private boolean bound;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OpenBitmessageLinkActivity.this.service = new Messenger(service);
            OpenBitmessageLinkActivity.this.bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_bitmessage_link);

        final TextView addressView = (TextView) findViewById(R.id.address);
        final EditText label = (EditText) findViewById(R.id.label);
        final Switch importContact = (Switch) findViewById(R.id.import_contact);
        final Switch subscribe = (Switch) findViewById(R.id.subscribe);

        Uri uri = getIntent().getData();
        final String address = getAddress(uri);
        String[] parameters = getParameters(uri);
        for (String parameter : parameters) {
            String name = parameter.substring(0, 6).toLowerCase();
            if (name.startsWith("label")) {
                label.setText(parameter.substring(parameter.indexOf('=') + 1).trim());
            } else if (name.startsWith("action")) {
                parameter = parameter.toLowerCase();
                importContact.setChecked(parameter.contains("add"));
                subscribe.setChecked(parameter.contains("subscribe"));
            }
        }

        addressView.setText(address);


        final Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        final Button ok = (Button) findViewById(R.id.do_import);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmessageAddress bmAddress = new BitmessageAddress(address);
                bmAddress.setAlias(label.getText().toString());

                final int what;
                if (subscribe.isChecked() && importContact.isChecked())
                    what = MSG_SUBSCRIBE_AND_ADD_CONTACT;
                else if (subscribe.isChecked())
                    what = MSG_SUBSCRIBE;
                else if (importContact.isChecked())
                    what = MSG_ADD_CONTACT;
                else
                    what = 0;

                if (what != 0) {
                    try {
                        Message message = Message.obtain(null, what);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(DATA_FIELD_ADDRESS, bmAddress);
                        message.setData(bundle);
                        service.send(message);
                    } catch (RemoteException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    private String getAddress(Uri uri) {
        StringBuilder result = new StringBuilder();
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        if (!schemeSpecificPart.startsWith("BM-")) {
            result.append("BM-");
        }
        if (schemeSpecificPart.contains("?")) {
            result.append(schemeSpecificPart.substring(0, schemeSpecificPart.indexOf('?')));
        } else if (schemeSpecificPart.contains("#")) {
            result.append(schemeSpecificPart.substring(0, schemeSpecificPart.indexOf('#')));
        } else {
            result.append(schemeSpecificPart);
        }
        return result.toString();
    }

    private String[] getParameters(Uri uri) {
        int index = uri.getSchemeSpecificPart().indexOf('?');
        if (index >= 0) {
            String parameterPart = uri.getSchemeSpecificPart().substring(index + 1);
            return parameterPart.split("&");
        } else {
            return new String[0];
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BitmessageService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        super.onStop();
    }
}
