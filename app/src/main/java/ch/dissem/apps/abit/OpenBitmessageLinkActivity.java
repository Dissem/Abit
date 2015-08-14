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
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;

public class OpenBitmessageLinkActivity extends AppCompatActivity {

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
                BitmessageContext bmc = Singleton.getBitmessageContext(OpenBitmessageLinkActivity.this);
                BitmessageAddress bmAddress = new BitmessageAddress(address);
                bmAddress.setAlias(label.getText().toString());
                if (subscribe.isChecked()) {
                    bmc.addSubscribtion(bmAddress);
                }
                if (importContact.isChecked()) {
                    bmc.addContact(bmAddress);
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
}
