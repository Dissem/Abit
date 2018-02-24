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
import ch.dissem.apps.abit.repository.AndroidNodeRegistry
import ch.dissem.apps.abit.repository.SqlHelper
import ch.dissem.bitmessage.entity.valueobject.NetworkAddress
import ch.dissem.bitmessage.ports.NodeRegistry
import ch.dissem.bitmessage.utils.UnixTime.now
import org.hamcrest.Matchers.empty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.*

/**
 * Please note that some tests fail if there is no internet connection,
 * as the initial nodes' IP addresses are determined by DNS lookup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LOLLIPOP], packageName = "ch.dissem.apps.abit")
class AndroidNodeRegistryTest : TestBase() {
    private lateinit var registry: NodeRegistry

    @Before
    fun setUp() {
        RuntimeEnvironment.application.deleteDatabase(SqlHelper.DATABASE_NAME)
        val sqlHelper = SqlHelper(RuntimeEnvironment.application)

        registry = AndroidNodeRegistry(sqlHelper)

        registry.offerAddresses(Arrays.asList(
            createAddress(lastByte = 1),
            createAddress(lastByte = 2),
            createAddress(lastByte = 3),
            createAddress(lastByte = 4, stream = 2)
        ))
    }

    @Test
    fun `ensure getKnownNodes() without streams yields empty`() =
        assertThat(registry.getKnownAddresses(10), empty<NetworkAddress>())

    @Test
    fun `ensure predefined node is returned when database is empty`() {
        RuntimeEnvironment.application.deleteDatabase(SqlHelper.DATABASE_NAME)

        val sqlHelper = SqlHelper(RuntimeEnvironment.application)
        registry = AndroidNodeRegistry(sqlHelper)

        val knownAddresses = registry.getKnownAddresses(2, 1)
        assertEquals(1, knownAddresses.size.toLong())
    }

    @Test
    fun `ensure known addresses are retrieved`() {
        var knownAddresses = registry.getKnownAddresses(2, 1)
        assertEquals(2, knownAddresses.size.toLong())

        knownAddresses = registry.getKnownAddresses(1000, 1)
        assertEquals(3, knownAddresses.size.toLong())
    }

    @Test
    fun `ensure offered addresses are added`() {
        registry.offerAddresses(Arrays.asList(
            createAddress(lastByte = 1),
            createAddress(lastByte = 10),
            createAddress(lastByte = 11)
        ))

        var knownAddresses = registry.getKnownAddresses(1000, 1)
        assertEquals(5, knownAddresses.size.toLong())

        registry.offerAddresses(listOf(createAddress(lastByte = 1, port = 8445)))

        knownAddresses = registry.getKnownAddresses(1000, 1)
        assertEquals(6, knownAddresses.size.toLong())
    }

    private fun createAddress(lastByte: Int, port: Int = 8444, stream: Long = 1, time: Long = now) =
        NetworkAddress.Builder()
            .ipv6(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, lastByte)
            .port(port)
            .stream(stream)
            .time(time)
            .build()
}
