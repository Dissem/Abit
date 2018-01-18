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

package ch.dissem.apps.abit.synchronization

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
class StubProvider : ContentProvider() {

    /**
     * Always return true, indicating that the
     * provider loaded correctly.
     */
    override fun onCreate() = true

    /**
     * Return no type for MIME type
     */
    override fun getType(uri: Uri) = null

    /**
     * query() always returns no results
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?) = null

    /**
     * insert() always returns null (no URI)
     */
    override fun insert(uri: Uri, values: ContentValues?) = null

    /**
     * delete() always returns "no rows affected" (0)
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    /**
     * update() always returns "no rows affected" (0)
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?) = 0

    companion object {
        const val AUTHORITY = "ch.dissem.apps.abit.provider"
    }
}
