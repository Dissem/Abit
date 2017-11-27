/*
 * Copyright 2017 Christian Basler
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

import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.InternalContext
import ch.dissem.bitmessage.Preferences
import ch.dissem.bitmessage.cryptography.sc.SpongyCryptography
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.ObjectMessage
import ch.dissem.bitmessage.entity.payload.V4Pubkey
import ch.dissem.bitmessage.entity.valueobject.InventoryVector
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import ch.dissem.bitmessage.factory.Factory
import ch.dissem.bitmessage.ports.*
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import org.junit.Assert
import org.junit.BeforeClass
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

open class TestBase {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            mockedInternalContext(
                cryptography = SpongyCryptography(),
                proofOfWorkEngine = MultiThreadedPOWEngine()
            )
        }

        fun mockedInternalContext(
            cryptography: Cryptography = mock {},
            inventory: Inventory = mock {},
            nodeRegistry: NodeRegistry = mock {},
            networkHandler: NetworkHandler = mock {},
            addressRepository: AddressRepository = mock {},
            labelRepository: LabelRepository = mock {},
            messageRepository: MessageRepository = mock {},
            proofOfWorkRepository: ProofOfWorkRepository = mock {},
            proofOfWorkEngine: ProofOfWorkEngine = mock {},
            customCommandHandler: CustomCommandHandler = mock {},
            listener: BitmessageContext.Listener = mock {},
            labeler: Labeler = mock {},
            port: Int = 0,
            connectionTTL: Long = 0,
            connectionLimit: Int = 0
        ) = spy(InternalContext(
            cryptography,
            inventory,
            nodeRegistry,
            networkHandler,
            addressRepository,
            labelRepository,
            messageRepository,
            proofOfWorkRepository,
            proofOfWorkEngine,
            customCommandHandler,
            listener,
            labeler,
            Preferences().apply {
                this.port = port
                this.connectionTTL = connectionTTL
                this.connectionLimit = connectionLimit
            }
        ))

        fun randomInventoryVector(): InventoryVector {
            val bytes = ByteArray(32)
            RANDOM.nextBytes(bytes)
            return InventoryVector(bytes)
        }

        private fun getResource(resourceName: String) =
            TestBase::class.java.classLoader.getResourceAsStream(resourceName)

        private fun loadObjectMessage(version: Int, resourceName: String): ObjectMessage {
            val data = getBytes(resourceName)
            val `in` = ByteArrayInputStream(data)
            return Factory.getObjectMessage(version, `in`, data.size) ?: throw NoSuchElementException("error loading object message")
        }

        private fun getBytes(resourceName: String): ByteArray {
            val `in` = getResource(resourceName)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len = `in`.read(buffer)
            while (len != -1) {
                out.write(buffer, 0, len)
                len = `in`.read(buffer)
            }
            return out.toByteArray()
        }

        fun loadIdentity(address: String): BitmessageAddress {
            val privateKey = PrivateKey.read(getResource(address + ".privkey"))
            val identity = BitmessageAddress(privateKey)
            Assert.assertEquals(address, identity.address)
            return identity
        }

        fun loadContact(): BitmessageAddress {
            val address = BitmessageAddress("BM-2cXxfcSetKnbHJX2Y85rSkaVpsdNUZ5q9h")
            val objectMessage = loadObjectMessage(3, "V4Pubkey.payload")
            objectMessage.decrypt(address.publicDecryptionKey)
            address.pubkey = objectMessage.payload as V4Pubkey
            return address
        }

        private val RANDOM = Random()
    }
}
