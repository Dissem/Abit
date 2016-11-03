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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static ch.dissem.apps.abit.ImportIdentitiesFragment.WIF_DATA;

/**
 * @author Christian Basler
 */

public class InputWifFragment extends Fragment {
    private TextView wifData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_input, container, false);
        wifData = (TextView) view.findViewById(R.id.wif_input);

        view.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(WIF_DATA, wifData.getText().toString());

                ImportIdentitiesFragment fragment = new ImportIdentitiesFragment();
                fragment.setArguments(bundle);

                getFragmentManager().beginTransaction()
                    .replace(R.id.content, fragment)
                    .commit();
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.import_input_data, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
        dialog.setTitle(getString(R.string.select_file_title));
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files.length > 0) {
                    try (InputStream in = new FileInputStream(files[0])) {
                        ByteArrayOutputStream data = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        //noinspection ConstantConditions
                        while ((length = in.read(buffer)) != -1) {
                            data.write(buffer, 0, length);
                        }
                        wifData.setText(data.toString("UTF-8"));
                    } catch (IOException e) {
                        Toast.makeText(
                            getActivity(),
                            R.string.error_loading_data,
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        });
        dialog.show();
        return true;
    }
}
