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
import ch.dissem.apps.abit.repository.AndroidAddressRepository
import ch.dissem.apps.abit.repository.SqlHelper
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.Pubkey.Feature.DOES_ACK
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LOLLIPOP], packageName = "ch.dissem.apps.abit")
class AndroidAddressRepositoryTest : TestBase() {
    private val contactA = "BM-2cW7cD5cDQJDNkE7ibmyTxfvGAmnPqa9Vt"
    private val contactB = "BM-2cTtkBnb4BUYDndTKun6D9PjtueP2h1bQj"
    private val contactC = "BM-2cV5f9EpzaYARxtoruSpa6pDoucSf9ZNke"

    private lateinit var identityA: String
    private lateinit var identityB: String

    private lateinit var repo: AndroidAddressRepository

    @Before
    fun setUp() {
        RuntimeEnvironment.application.deleteDatabase(SqlHelper.DATABASE_NAME)
        val sqlHelper = SqlHelper(RuntimeEnvironment.application)

        repo = AndroidAddressRepository(sqlHelper)

        repo.save(BitmessageAddress(contactA))
        repo.save(BitmessageAddress(contactB))
        repo.save(BitmessageAddress(contactC))

        BitmessageAddress(PrivateKey(false, 1, 1000, 1000, DOES_ACK)).let {
            repo.save(it)
            identityA = it.address
        }
        BitmessageAddress(PrivateKey(false, 1, 1000, 1000)).let {
            repo.save(it)
            identityB = it.address
        }
    }

    @Test
    fun `ensure contact can be found`() {
        val address = BitmessageAddress(contactA)
        assertEquals(4, address.version)
        assertEquals(address, repo.findContact(address.tag!!))
        assertNull(repo.findIdentity(address.tag!!))
    }

    @Test
    fun `ensure identity can be found`() {
        val identity = BitmessageAddress(identityA)
        assertEquals(4, identity.version)
        assertNull(repo.findContact(identity.tag!!))

        val storedIdentity = repo.findIdentity(identity.tag!!)
        assertEquals(identity, storedIdentity)
        assertTrue(storedIdentity!!.has(DOES_ACK))
    }

    @Test
    fun `ensure identities are retrieved`() {
        val identities = repo.getIdentities()
        assertEquals(2, identities.size.toLong())
        for (identity in identities) {
            assertNotNull(identity.privateKey)
        }
    }

    @Test
    fun `ensure subscriptions are retrieved`() {
        addSubscription("BM-2cXxfcSetKnbHJX2Y85rSkaVpsdNUZ5q9h")
        addSubscription("BM-2D9Vc5rFxxR5vTi53T9gkLfemViHRMVLQZ")
        addSubscription("BM-2D9QKN4teYRvoq2fyzpiftPh9WP9qggtzh")
        val subscriptions = repo.getSubscriptions()
        assertEquals(3, subscriptions.size.toLong())
    }

    @Test
    fun `ensure subscriptions are retrieved for given version`() {
        addSubscription("BM-2cXxfcSetKnbHJX2Y85rSkaVpsdNUZ5q9h")
        addSubscription("BM-2D9Vc5rFxxR5vTi53T9gkLfemViHRMVLQZ")
        addSubscription("BM-2D9QKN4teYRvoq2fyzpiftPh9WP9qggtzh")

        var subscriptions = repo.getSubscriptions(5)

        assertEquals(1, subscriptions.size.toLong())

        subscriptions = repo.getSubscriptions(4)
        assertEquals(2, subscriptions.size.toLong())
    }

    @Test
    fun `ensure contacts are retrieved`() {
        val contacts = repo.getContacts()
        assertEquals(3, contacts.size.toLong())
        for (contact in contacts) {
            assertNull(contact.privateKey)
        }
    }

    @Test
    fun `ensure new address is saved`() {
        repo.save(BitmessageAddress(PrivateKey(false, 1, 1000, 1000)))
        val identities = repo.getIdentities()
        assertEquals(3, identities.size.toLong())
    }

    @Test
    fun `ensure existing address is updated`() {
        var address = repo.getAddress(contactA)
        address!!.alias = "Test-Alias"
        repo.save(address)
        address = repo.getAddress(address.address)
        assertEquals("Test-Alias", address!!.alias)
    }

    @Test
    fun `ensure existing keys are not deleted`() {
        val address = BitmessageAddress(identityA)
        address.alias = "Test"
        repo.save(address)
        val identityA = repo.getAddress(identityA)
        assertNotNull(identityA!!.pubkey)
        assertNotNull(identityA.privateKey)
        assertEquals("Test", identityA.alias)
        assertFalse(identityA.isChan)
    }

    @Test
    fun `ensure new chan is saved and updated`() {
        val chan = BitmessageAddress.chan(1, "test")
        repo.save(chan)
        var address = repo.getAddress(chan.address)
        assertNotNull(address)
        assertTrue(address!!.isChan)

        address.alias = "Test"
        repo.save(address)

        address = repo.getAddress(chan.address)
        assertNotNull(address)
        assertTrue(address!!.isChan)
        assertEquals("Test", address.alias)
    }

    @Test
    fun `ensure address is removed`() {
        val address = repo.getAddress(identityA)
        repo.remove(address!!)
        assertNull(repo.getAddress(identityA))
    }

    @Test
    fun `ensure address can be retrieved`() {
        val address = repo.getAddress(identityA)
        assertNotNull(address)
        assertNotNull(address!!.privateKey)
    }

    private fun addSubscription(address: String) {
        val subscription = BitmessageAddress(address)
        subscription.isSubscribed = true
        repo.save(subscription)
    }
}
