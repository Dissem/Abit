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

package ch.dissem.apps.abit.util

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import ch.dissem.apps.abit.R
import ch.dissem.bitmessage.entity.Plaintext
import java.io.IOException
import java.util.*

/**
 * Helper class to work with Assets.
 */
object Assets {
    fun readSqlStatements(ctx: Context, name: String): List<String> {
        try {
            val `in` = ctx.assets.open(name)
            val scanner = Scanner(`in`, "UTF-8").useDelimiter(";")
            val result = LinkedList<String>()
            while (scanner.hasNext()) {
                val statement = scanner.next().trim { it <= ' ' }
                if ("" != statement) {
                    result.add(statement)
                }
            }
            return result
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @DrawableRes
    fun getStatusDrawable(status: Plaintext.Status): Int {
        when (status) {
            Plaintext.Status.RECEIVED -> return 0
            Plaintext.Status.DRAFT -> return R.drawable.draft
            Plaintext.Status.PUBKEY_REQUESTED -> return R.drawable.public_key
            Plaintext.Status.DOING_PROOF_OF_WORK -> return R.drawable.ic_notification_proof_of_work
            Plaintext.Status.SENT -> return R.drawable.sent
            Plaintext.Status.SENT_ACKNOWLEDGED -> return R.drawable.sent_acknowledged
            else -> return 0
        }
    }

    @StringRes
    fun getStatusString(status: Plaintext.Status): Int {
        when (status) {
            Plaintext.Status.RECEIVED -> return R.string.status_received
            Plaintext.Status.DRAFT -> return R.string.status_draft
            Plaintext.Status.PUBKEY_REQUESTED -> return R.string.status_public_key
            Plaintext.Status.DOING_PROOF_OF_WORK -> return R.string.proof_of_work_title
            Plaintext.Status.SENT -> return R.string.status_sent
            Plaintext.Status.SENT_ACKNOWLEDGED -> return R.string.status_sent_acknowledged
            else -> return 0
        }
    }
}
