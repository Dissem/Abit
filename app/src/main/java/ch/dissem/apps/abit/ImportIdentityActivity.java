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

import static ch.dissem.apps.abit.ImportIdentitiesFragment.WIF_DATA;

/**
 * @author Christian Basler
 */

public class ImportIdentityActivity extends DetailActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String wifData;
        if (savedInstanceState == null) {
            wifData = null;
        } else {
            wifData = savedInstanceState.getString(WIF_DATA);
        }
        if (wifData == null) {
            getFragmentManager().beginTransaction()
                .replace(R.id.content, new InputWifFragment())
                .commit();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(WIF_DATA, wifData);

            ImportIdentitiesFragment fragment = new ImportIdentitiesFragment();
            fragment.setArguments(bundle);

            getFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
        }
    }

}
