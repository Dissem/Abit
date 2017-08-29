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

package ch.dissem.apps.abit

import android.app.Fragment
import android.os.Bundle
import android.view.*
import android.widget.Toast
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import kotlinx.android.synthetic.main.fragment_import_input.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * @author Christian Basler
 */
class InputWifFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View =
            inflater.inflate(R.layout.fragment_import_input, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        next.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(ImportIdentitiesFragment.WIF_DATA, wif_input.text.toString())

            val fragment = ImportIdentitiesFragment().apply {
                arguments = bundle
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.content, fragment)
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.import_input_data, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.extensions = null
        val dialog = FilePickerDialog(activity, properties)
        dialog.setTitle(getString(R.string.select_file_title))
        dialog.setDialogSelectionListener { files ->
            if (files.isNotEmpty()) {
                try {
                    FileInputStream(files[0]).use { inputStream ->
                        val data = ByteArrayOutputStream()
                        val buffer = ByteArray(1024)

                        var length: Int = inputStream.read(buffer)

                        while (length != -1) {
                            data.write(buffer, 0, length)
                            length = inputStream.read(buffer)
                        }
                        wif_input.setText(data.toByteArray().toString())
                    }
                } catch (e: IOException) {
                    Toast.makeText(
                            activity,
                            R.string.error_loading_data,
                            Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
        dialog.show()
        return true
    }
}
