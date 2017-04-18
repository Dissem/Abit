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

package ch.dissem.apps.abit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.IIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.exception.ApplicationException;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.util.Base64.URL_SAFE;

/**
 * Some helper methods to work with drawables.
 */
public class Drawables {
    private static final Logger LOG = LoggerFactory.getLogger(Drawables.class);

    private static final int QR_CODE_SIZE = 350;

    public static MenuItem addIcon(Context ctx, Menu menu, int menuItem, IIcon icon) {
        MenuItem item = menu.findItem(menuItem);
        item.setIcon(new IconicsDrawable(ctx, icon).colorRes(R.color.colorPrimaryDarkText).actionBar());
        return item;
    }

    public static Bitmap toBitmap(Identicon identicon, int size) {
        return toBitmap(identicon, size, size);
    }

    public static Bitmap toBitmap(Identicon identicon, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        identicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        identicon.draw(canvas);
        return bitmap;
    }

    public static Bitmap qrCode(BitmessageAddress address) {
        StringBuilder link = new StringBuilder("bitmessage:");
        link.append(address.getAddress());
        if (address.getAlias() != null) {
            link.append("?label=").append(address.getAlias());
        }
        if (address.getPubkey() != null) {
            link.append(address.getAlias() == null ? '?' : '&');
            ByteArrayOutputStream pubkey = new ByteArrayOutputStream();
            try {
                address.getPubkey().writeUnencrypted(pubkey);
            } catch (IOException e) {
                throw new ApplicationException(e);
            }
            link.append("pubkey=").append(Base64.encodeToString(pubkey.toByteArray(), URL_SAFE));
        }
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(link.toString(),
                BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, null);
        } catch (WriterException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h);
        return bitmap;
    }
}
