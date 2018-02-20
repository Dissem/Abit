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

import android.os.Build.VERSION_CODES.LOLLIPOP
import ch.dissem.apps.abit.repository.*
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.ObjectMessage
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.payload.GenericPayload
import ch.dissem.bitmessage.entity.payload.GetPubkey
import ch.dissem.bitmessage.entity.payload.ObjectPayload
import ch.dissem.bitmessage.entity.payload.Pubkey
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import ch.dissem.bitmessage.ports.AddressRepository
import ch.dissem.bitmessage.ports.MessageRepository
import ch.dissem.bitmessage.ports.ProofOfWorkRepository
import ch.dissem.bitmessage.utils.Singleton.cryptography
import ch.dissem.bitmessage.utils.UnixTime
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.properties.Delegates

/**
 * @author Christian Basler
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LOLLIPOP], packageName = "ch.dissem.apps.abit")
class AndroidProofOfWorkRepositoryTest : TestBase() {
    private lateinit var repo: ProofOfWorkRepository
    private lateinit var addressRepo: AddressRepository
    private lateinit var messageRepo: MessageRepository

    private var initialHash1: ByteArray by Delegates.notNull()
    private var initialHash2: ByteArray by Delegates.notNull()

    @Before
    fun setUp() {
        RuntimeEnvironment.application.deleteDatabase(SqlHelper.DATABASE_NAME)
        val sqlHelper = SqlHelper(RuntimeEnvironment.application)

        addressRepo = AndroidAddressRepository(sqlHelper)
        messageRepo = AndroidMessageRepository(sqlHelper)
        repo = AndroidProofOfWorkRepository(sqlHelper)
        mockedInternalContext(
            addressRepository = addressRepo,
            labelRepository = AndroidLabelRepository(sqlHelper, RuntimeEnvironment.application),
            messageRepository = messageRepo,
            proofOfWorkRepository = repo,
            cryptography = cryptography()
        )

        repo.putObject(
            objectMessage = ObjectMessage.Builder()
                .payload(GetPubkey(BitmessageAddress("BM-2DAjcCFrqFrp88FUxExhJ9kPqHdunQmiyn")))
                .build(),
            nonceTrialsPerByte = 1000,
            extraBytes = 1000
        )
        initialHash1 = repo.getItems()[0]

        val sender = loadIdentity("BM-2cSqjfJ8xK6UUn5Rw3RpdGQ9RsDkBhWnS8")
        val recipient = loadContact()
        addressRepo.save(sender)
        addressRepo.save(recipient)
        val plaintext = Plaintext.Builder(Plaintext.Type.MSG)
            .ackData(cryptography().randomBytes(32))
            .from(sender)
            .to(recipient)
            .message("Subject", "Message")
            .status(Plaintext.Status.DOING_PROOF_OF_WORK)
            .build()
        messageRepo.save(plaintext)
        plaintext.ackMessage!!.let { ackMessage ->
            initialHash2 = cryptography().getInitialHash(ackMessage)
            repo.putObject(ProofOfWorkRepository.Item(
                objectMessage = ackMessage,
                nonceTrialsPerByte = 1000, extraBytes = 1000,
                expirationTime = UnixTime.now + 10 * UnixTime.MINUTE,
                message = plaintext
            ))
        }
    }

    @Test
    fun `ensure object is stored`() {
        val sizeBefore = repo.getItems().size
        repo.putObject(
            objectMessage = ObjectMessage.Builder()
                .payload(GetPubkey(BitmessageAddress("BM-2D9U2hv3YBMHM1zERP32anKfVKohyPN9x2")))
                .build(),
            nonceTrialsPerByte = 1000,
            extraBytes = 1000
        )
        assertThat(repo.getItems().size, `is`(sizeBefore + 1))
    }

    @Test
    fun `ensure ack objects are stored`() {
        val sizeBefore = repo.getItems().size
        val sender = loadIdentity("BM-2cSqjfJ8xK6UUn5Rw3RpdGQ9RsDkBhWnS8")
        val recipient = loadContact()
        addressRepo.save(sender)
        addressRepo.save(recipient)
        val plaintext = Plaintext.Builder(Plaintext.Type.MSG)
            .ackData(cryptography().randomBytes(32))
            .from(sender)
            .to(recipient)
            .message("Subject", "Message")
            .status(Plaintext.Status.DOING_PROOF_OF_WORK)
            .build()
        messageRepo.save(plaintext)
        plaintext.ackMessage!!.let { ackMessage ->
            repo.putObject(ProofOfWorkRepository.Item(
                objectMessage = ackMessage,
                nonceTrialsPerByte = 1000,
                extraBytes = 1000,
                expirationTime = UnixTime.now + 10 * UnixTime.MINUTE,
                message = plaintext
            ))
        }
        assertThat(repo.getItems().size, `is`(sizeBefore + 1))
    }

    @Test
    fun `ensure item can be retrieved`() {
        val item = repo.getItem(initialHash1)
        assertThat(item, notNullValue())
        assertThat<ObjectPayload>(item.objectMessage.payload, instanceOf<ObjectPayload>(GetPubkey::class.java))
        assertThat(item.nonceTrialsPerByte, `is`(1000L))
        assertThat(item.extraBytes, `is`(1000L))
    }

    @Test
    fun `ensure ack item can be retrieved`() {
        val item = repo.getItem(initialHash2)
        assertThat(item, notNullValue())
        assertThat<ObjectPayload>(item.objectMessage.payload, instanceOf<ObjectPayload>(GenericPayload::class.java))
        assertThat(item.nonceTrialsPerByte, `is`(1000L))
        assertThat(item.extraBytes, `is`(1000L))
        assertThat(item.expirationTime, not<Number>(0))
        assertThat(item.message, notNullValue())
        assertThat<PrivateKey>(item.message?.from?.privateKey, notNullValue())
        assertThat<Pubkey>(item.message?.to?.pubkey, notNullValue())
    }

    @Test(expected = RuntimeException::class)
    fun `ensure retrieving non-existing item causes exception`() {
        repo.getItem(ByteArray(0))
    }

    @Test
    fun `ensure item can be deleted`() {
        repo.removeObject(initialHash1)
        repo.removeObject(initialHash2)
        assertTrue(repo.getItems().isEmpty())
    }

    @Test
    fun `ensure deletion of non-existing item is handled silently`() {
        repo.removeObject(ByteArray(0))
    }
}
