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
object Strings {
    private val WHITESPACES = Pattern.compile("\\s+")

    private fun trim(string: CharSequence?, length: Int) = when {
        string == null -> ""
        string.length <= length -> string
        else -> string.subSequence(0, length)
    }

    /**
     * Trim the string to 200 characters and normalizes all whitespaces by replacing any sequence
     * of whitespace characters with a single space character.
     */
    fun prepareMessageExtract(string: CharSequence?): String = WHITESPACES.matcher(trim(string, 200)).replaceAll(" ")
}
