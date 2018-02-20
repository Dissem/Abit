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

package ch.dissem.apps.abit

import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint

import ch.dissem.bitmessage.entity.BitmessageAddress

/**
 * @author Christian Basler
 */
class Identicon(input: BitmessageAddress) : Drawable() {

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val hash = input.ripe
    private val isChan = input.isChan
    private val fields = Array(SIZE) { BooleanArray(SIZE) }.apply {
        for (row in 0 until SIZE) {
            if (!isChan || row < 5 || row > 6) {
                for (column in 0..CENTER_COLUMN) {
                    if ((row - SIZE / 2) * (row - SIZE / 2) + (column - SIZE / 2) * (column - SIZE / 2) < SIZE / 2 * SIZE / 2) {
                        this[row][column] = hash[(row * CENTER_COLUMN + column) % hash.size] >= 0
                        this[row][SIZE - column - 1] = this[row][column]
                    }
                }
            }
        }
    }
    private val color = Color.HSVToColor(floatArrayOf((Math.abs(hash[0] * hash[1] + hash[2]) % 360).toFloat(), 0.8f, 1.0f))
    private val background = Color.HSVToColor(floatArrayOf((Math.abs(hash[1] * hash[2] + hash[0]) % 360).toFloat(), 0.8f, 1.0f))
    private val textPaint = TextPaint().apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF607D8B.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    override fun draw(canvas: Canvas) {
        var x: Float
        var y: Float
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val cellWidth = width / SIZE.toFloat()
        val cellHeight = height / SIZE.toFloat()
        paint.color = background
        canvas.drawCircle(width / 2, height / 2, width / 2, paint)
        paint.color = color
        for (row in 0 until SIZE) {
            for (column in 0 until SIZE) {
                if (fields[row][column]) {
                    x = cellWidth * column
                    y = cellHeight * row
                    canvas.drawCircle(
                            x + cellWidth / 2, y + cellHeight / 2, cellHeight / 2,
                            paint
                    )
                }
            }
        }
        if (isChan) {
            textPaint.textSize = 2 * cellHeight
            canvas.drawText("[isChan]", width / 2, 6.7f * cellHeight, textPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    override fun getOpacity() = PixelFormat.TRANSPARENT

    companion object {
        private const val SIZE = 9
        private const val CENTER_COLUMN = 5
    }
}
