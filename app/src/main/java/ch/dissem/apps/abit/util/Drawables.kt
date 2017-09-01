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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.URL_SAFE
import android.view.Menu
import android.view.MenuItem
import ch.dissem.apps.abit.Identicon
import ch.dissem.apps.abit.R
import ch.dissem.bitmessage.entity.BitmessageAddress
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

/**
 * Some helper methods to work with drawables.
 */
object Drawables {
    private val LOG = LoggerFactory.getLogger(Drawables::class.java)

    private val QR_CODE_SIZE = 350

    fun addIcon(ctx: Context, menu: Menu, menuItem: Int, icon: IIcon): MenuItem {
        val item = menu.findItem(menuItem)
        item.icon = IconicsDrawable(ctx, icon).colorRes(R.color.colorPrimaryDarkText).actionBar()
        return item
    }

    fun toBitmap(identicon: Identicon, width: Int, height: Int = width): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        identicon.setBounds(0, 0, canvas.width, canvas.height)
        identicon.draw(canvas)
        return bitmap
    }

    fun qrCode(address: BitmessageAddress?): Bitmap? {
        if (address == null) {
            return null
        }
        val link = StringBuilder()
        link.append(Constants.BITMESSAGE_URL_SCHEMA)
        link.append(address.address)
        if (address.alias != null) {
            link.append("?label=").append(address.alias)
        }
        address.pubkey?.apply {
            link.append(if (address.alias == null) '?' else '&')
            val pubkey = ByteArrayOutputStream()
            writeUnencrypted(pubkey)
            link.append("pubkey=").append(Base64.encodeToString(pubkey.toByteArray(), URL_SAFE or NO_WRAP))

        }
        val result: BitMatrix
        try {
            result = MultiFormatWriter().encode(link.toString(),
                    BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, null)
        } catch (e: WriterException) {
            LOG.error(e.message, e)
            return null
        }

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) BLACK else WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h)
        return bitmap
    }
}
