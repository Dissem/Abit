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
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.entity.payload.V2Pubkey;
import ch.dissem.bitmessage.entity.payload.V3Pubkey;
import ch.dissem.bitmessage.entity.payload.V4Pubkey;

import static android.util.Base64.URL_SAFE;

public class CreateAddressActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(CreateAddressActivity.class);

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z]+)=(.*)$");
    private byte[] pubkeyBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri != null)
            setContentView(R.layout.activity_open_bitmessage_link);
        else
            setContentView(R.layout.activity_create_bitmessage_address);

        final TextView address = (TextView) findViewById(R.id.address);
        final EditText label = (EditText) findViewById(R.id.label);
        final Switch subscribe = (Switch) findViewById(R.id.subscribe);

        if (uri != null) {
            String addressText = getAddress(uri);
            String[] parameters = getParameters(uri);
            for (String parameter : parameters) {
                Matcher matcher = KEY_VALUE_PATTERN.matcher(parameter);
                if (matcher.find()) {
                    String key = matcher.group(1).toLowerCase();
                    String value = matcher.group(2);
                    switch (key) {
                        case "label":
                            label.setText(value.trim());
                            break;
                        case "action":
                            subscribe.setChecked(value.trim().equalsIgnoreCase("subscribe"));
                            break;
                        case "pubkey":
                            pubkeyBytes = Base64.decode(value, URL_SAFE);
                            break;
                        default:
                            LOG.debug("Unknown attribute: " + key + "=" + value);
                            break;
                    }
                }
            }

            address.setText(addressText);
        }

        final Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        findViewById(R.id.do_import).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOK(address, label, subscribe);
            }
        });
    }


    private void onOK(TextView address, EditText label, Switch subscribe) {
        String addressText = String.valueOf(address.getText()).trim();
        try {
            BitmessageAddress bmAddress = new BitmessageAddress(addressText);
            bmAddress.setAlias(label.getText().toString());

            BitmessageContext bmc = Singleton.getBitmessageContext
                (CreateAddressActivity.this);
            bmc.addContact(bmAddress);
            if (subscribe.isChecked()) {
                bmc.addSubscribtion(bmAddress);
            }
            if (pubkeyBytes != null) {
                try {
                    final Pubkey pubkey;
                    InputStream pubkeyStream = new ByteArrayInputStream(pubkeyBytes);
                    long stream = bmAddress.getStream();
                    switch ((int) bmAddress.getVersion()) {
                        case 2:
                            pubkey = V2Pubkey.read(pubkeyStream, stream);
                            break;
                        case 3:
                            pubkey = V3Pubkey.read(pubkeyStream, stream);
                            break;
                        case 4:
                            pubkey = new V4Pubkey(V3Pubkey.read(pubkeyStream, stream));
                            break;
                        default:
                            pubkey = null;
                            break;
                    }
                    if (pubkey != null) {
                        bmAddress.setPubkey(pubkey);
                    }
                } catch (Exception ignore) {
                }
            }

            setResult(Activity.RESULT_OK);
            finish();
        } catch (RuntimeException e) {
            address.setError(getString(R.string.error_illegal_address));
        }
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
