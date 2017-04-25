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

package ch.dissem.apps.abit.util;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.Plaintext;

/**
 * Helper class to work with Assets.
 */
public class Assets {
    public static List<String> readSqlStatements(Context ctx, String name) {
        try {
            InputStream in = ctx.getAssets().open(name);
            Scanner scanner = new Scanner(in, "UTF-8").useDelimiter(";");
            List<String> result = new LinkedList<>();
            while (scanner.hasNext()) {
                String statement = scanner.next().trim();
                if (!"".equals(statement)) {
                    result.add(statement);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DrawableRes
    public static int getStatusDrawable(Plaintext.Status status) {
        switch (status) {
            case RECEIVED:
                return 0;
            case DRAFT:
                return R.drawable.draft;
            case PUBKEY_REQUESTED:
                return R.drawable.public_key;
            case DOING_PROOF_OF_WORK:
                return R.drawable.ic_notification_proof_of_work;
            case SENT:
                return R.drawable.sent;
            case SENT_ACKNOWLEDGED:
                return R.drawable.sent_acknowledged;
            default:
                return 0;
        }
    }

    @StringRes
    public static int getStatusString(Plaintext.Status status) {
        switch (status) {
            case RECEIVED:
                return R.string.status_received;
            case DRAFT:
                return R.string.status_draft;
            case PUBKEY_REQUESTED:
                return R.string.status_public_key;
            case DOING_PROOF_OF_WORK:
                return R.string.proof_of_work_title;
            case SENT:
                return R.string.status_sent;
            case SENT_ACKNOWLEDGED:
                return R.string.status_sent_acknowledged;
            default:
                return 0;
        }
    }
}
