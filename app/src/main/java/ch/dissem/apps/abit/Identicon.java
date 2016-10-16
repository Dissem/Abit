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

package ch.dissem.apps.abit;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * @author Christian Basler
 */
public class Identicon extends Drawable {
    private static final int SIZE = 9;
    private static final int CENTER_COLUMN = 5;

    private final Paint paint;
    private int color;
    private int background;
    private boolean[][] fields;
    private boolean chan;
    private final TextPaint textPaint;

    public Identicon(BitmessageAddress input) {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF607D8B);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        chan = input.isChan();

        byte[] hash = input.getRipe();

        fields = new boolean[SIZE][SIZE];
        color = Color.HSVToColor(new float[]{
            Math.abs(hash[0] * hash[1] + hash[2]) % 360,
            0.8f,
            1.0f
        });
        background = Color.HSVToColor(new float[]{
            Math.abs(hash[1] * hash[2] + hash[0]) % 360,
            0.8f,
            1.0f
        });

        for (int row = 0; row < SIZE; row++) {
            if (!chan || row < 5 || row > 6) {
                for (int column = 0; column <= CENTER_COLUMN; column++) {
                    if (
                        (row - SIZE / 2) * (row - SIZE / 2)
                            + (column - SIZE / 2) * (column - SIZE / 2)
                            < SIZE / 2 * SIZE / 2
                        ) {
                        fields[row][column] = hash[(row * CENTER_COLUMN + column) % hash.length]
                            >= 0;
                        fields[row][SIZE - column - 1] = fields[row][column];
                    }
                }
            }
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float x, y;
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float cellWidth = width / (float) SIZE;
        float cellHeight = height / (float) SIZE;
        paint.setColor(background);
        canvas.drawCircle(width / 2, height / 2, width / 2, paint);
        paint.setColor(color);
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (fields[row][column]) {
                    x = cellWidth * column;
                    y = cellHeight * row;
                    canvas.drawCircle(
                        x + cellWidth / 2, y + cellHeight / 2, cellHeight / 2,
                        paint
                    );
                }
            }
        }
        if (chan) {
            textPaint.setTextSize(2 * cellHeight);
            canvas.drawText("[chan]", width / 2, 6.7f * cellHeight, textPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
