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

package ch.dissem.apps.abit.util

import java.util.regex.Pattern

/**
 * @author Christian Basler
 */
object Constants {
    const val PREFERENCE_WIFI_ONLY = "wifi_only"
    const val PREFERENCE_TRUSTED_NODE = "trusted_node"
    const val PREFERENCE_SYNC_TIMEOUT = "sync_timeout"
    const val PREFERENCE_SERVER_POW = "server_pow"
    const val PREFERENCE_FULL_NODE = "full_node"
    const val PREFERENCE_REQUEST_ACK = "request_acknowledgments"
    const val PREFERENCE_POW_AVERAGE = "average_pow_time_ms"
    const val PREFERENCE_POW_COUNT = "pow_count"

    const val BITMESSAGE_URL_SCHEMA = "bitmessage:"

    val BITMESSAGE_ADDRESS_PATTERN = Pattern.compile("\\bBM-[a-zA-Z0-9]+\\b")!!
}
