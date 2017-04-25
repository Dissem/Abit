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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.Plaintext;

import static android.app.Activity.RESULT_OK;
import static ch.dissem.apps.abit.ComposeMessageActivity.EXTRA_ENCODING;
import static ch.dissem.bitmessage.entity.Plaintext.Encoding.EXTENDED;
import static ch.dissem.bitmessage.entity.Plaintext.Encoding.SIMPLE;

/**
 * @author Christian Basler
 */

public class SelectEncodingDialogFragment extends AppCompatDialogFragment {
    private static final Logger LOG = LoggerFactory.getLogger(SelectEncodingDialogFragment.class);
    private Plaintext.Encoding encoding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(EXTRA_ENCODING)) {
            encoding = (Plaintext.Encoding) getArguments().getSerializable(EXTRA_ENCODING);
        }
        if (encoding == null) {
            encoding = SIMPLE;
        }
        getDialog().setTitle(R.string.select_encoding_title);
        View view = inflater.inflate(R.layout.dialog_select_message_encoding, container, false);
        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        switch (encoding) {
            case SIMPLE:
                radioGroup.check(R.id.simple);
                break;
            case EXTENDED:
                radioGroup.check(R.id.extended);
                break;
            default:
                LOG.warn("Unexpected encoding: " + encoding);
                break;
        }
        view.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.extended:
                        encoding = EXTENDED;
                        break;
                    case R.id.simple:
                        encoding = SIMPLE;
                        break;
                    default:
                        dismiss();
                        return;
                }
                Intent result = new Intent();
                result.putExtra(EXTRA_ENCODING, encoding);
                getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, result);
                dismiss();
            }
        });
        view.findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }
}
