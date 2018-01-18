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

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Bundle

/**
 * Implement AbstractAccountAuthenticator and stub out all
 * of its methods
 */
class Authenticator(context: Context) : AbstractAccountAuthenticator(context) {

    override fun editProperties(r: AccountAuthenticatorResponse, s: String) =
            throw UnsupportedOperationException("Editing properties is not supported")

    // Don't add additional accounts
    @Throws(NetworkErrorException::class)
    override fun addAccount(r: AccountAuthenticatorResponse, s: String, s2: String, strings: Array<String>, bundle: Bundle) = null

    // Ignore attempts to confirm credentials
    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(r: AccountAuthenticatorResponse, account: Account, bundle: Bundle) = null

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(r: AccountAuthenticatorResponse, account: Account, s: String, bundle: Bundle) =
            throw UnsupportedOperationException("Getting an authentication token is not supported")

    override fun getAuthTokenLabel(s: String) =
            throw UnsupportedOperationException("Getting a label for the auth token is not supported")

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(r: AccountAuthenticatorResponse, account: Account, s: String, bundle: Bundle) =
            throw UnsupportedOperationException("Updating user credentials is not supported")

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(r: AccountAuthenticatorResponse, account: Account, strings: Array<String>) =
            throw UnsupportedOperationException("Checking features for the account is not supported")

    companion object {
        val ACCOUNT_SYNC = Account("Bitmessage", "ch.dissem.bitmessage")
        val ACCOUNT_POW = Account("Proof of Work ", "ch.dissem.bitmessage")
    }
}
