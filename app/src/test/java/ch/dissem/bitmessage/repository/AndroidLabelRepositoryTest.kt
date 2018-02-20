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

package ch.dissem.bitmessage.repository

import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import ch.dissem.apps.abit.repository.AndroidLabelRepository
import ch.dissem.apps.abit.repository.SqlHelper
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.ports.LabelRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LOLLIPOP], packageName = "ch.dissem.apps.abit")
class AndroidLabelRepositoryTest : TestBase() {

    private lateinit var repo: LabelRepository


    @Before
    fun setUp() {
        RuntimeEnvironment.application.deleteDatabase(SqlHelper.DATABASE_NAME)
        val sqlHelper = SqlHelper(RuntimeEnvironment.application)

        repo = AndroidLabelRepository(sqlHelper, RuntimeEnvironment.application)
    }

    @Test
    fun `ensure labels are retrieved`() {
        val labels = repo.getLabels()
        assertEquals(7, labels.size.toLong())
    }

    @Test
    fun `ensure labels can be retrieved by type`() {
        val labels = repo.getLabels(Label.Type.INBOX)
        assertEquals(1, labels.size.toLong())
        assertEquals("Inbox", labels[0].toString())
    }

}
