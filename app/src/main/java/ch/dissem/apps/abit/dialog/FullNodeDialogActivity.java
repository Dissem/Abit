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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.util.NetworkUtils;
import ch.dissem.apps.abit.util.Preferences;

/**
 * @author Christian Basler
 */

public class FullNodeDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_full_node);
        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Preferences.setWifiOnly(FullNodeDialogActivity.this, false);
                NetworkUtils.enableNode(getApplicationContext());
                finish();
            }
        });
        findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    NetworkUtils.scheduleNodeStart(getApplicationContext());
                }
                finish();
            }
        });
    }
}
