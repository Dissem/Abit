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
import android.support.annotation.ColorInt
import android.text.TextPaint
import ch.dissem.bitmessage.entity.BitmessageAddress
import org.jetbrains.anko.collections.forEachWithIndex
import kotlin.math.sqrt

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
    private val color = Color.HSVToColor(
        floatArrayOf(
            (Math.abs(hash[0] * hash[1] + hash[2]) % 360).toFloat(),
            0.8f,
            1.0f
        )
    )
    private val background = Color.HSVToColor(
        floatArrayOf(
            (Math.abs(hash[1] * hash[2] + hash[0]) % 360).toFloat(),
            0.8f,
            1.0f
        )
    )
    private val textPaint = TextPaint().apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF607D8B.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    override fun draw(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        draw(canvas, 0f, 0f, width, height)
    }

    internal fun draw(canvas: Canvas, offsetX: Float, offsetY: Float, width: Float, height: Float) {
        var x: Float
        var y: Float
        val cellWidth = width / SIZE.toFloat()
        val cellHeight = height / SIZE.toFloat()
        paint.color = background
        canvas.drawCircle(offsetX + width / 2, offsetY + height / 2, width / 2, paint)
        paint.color = color
        for (row in 0 until SIZE) {
            for (column in 0 until SIZE) {
                if (fields[row][column]) {
                    x = offsetX + cellWidth * column
                    y = offsetY + cellHeight * row
                    canvas.drawCircle(
                        x + cellWidth / 2, y + cellHeight / 2, cellHeight / 2,
                        paint
                    )
                }
            }
        }
        if (isChan) {
            textPaint.textSize = 2 * cellHeight
            canvas.drawText("[ chan ]", offsetX + width / 2, offsetY + 6.7f * cellHeight, textPaint)
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

class MultiIdenticon(input: List<BitmessageAddress>, @ColorInt private val backgroundColor: Int = 0xFFAEC2CC.toInt()) :
    Drawable() {

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = backgroundColor
    }

    private val identicons = input.sortedBy { it.isChan }.map { Identicon(it) }.take(4)

    override fun draw(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        when (identicons.size) {
            0 -> canvas.drawCircle(width / 2, height / 2, width / 2, paint)
            1 -> identicons.first().draw(canvas, 0f, 0f, width, height)
            2 -> {
                canvas.drawCircle(width / 2, height / 2, width / 2, paint)
                val w = width / 2
                val h = height / 2
                var x = 0f
                val y = height / 4
                identicons.forEach {
                    it.draw(canvas, x, y, w, h)
                    x += w
                }
            }
            3 -> {
                val scale = 2f / (1f + 2f * sqrt(3f))
                val w = width * scale
                val h = height * scale

                canvas.drawCircle(width / 2, height / 2, width / 2, paint)
                identicons[0].draw(canvas, (width - w) / 2, 0f, w, h)
                identicons[1].draw(canvas, (width - 2 * w) / 2, h * sqrt(3f) / 2, w, h)
                identicons[2].draw(canvas, width / 2, h * sqrt(3f) / 2, w, h)
            }
            4 -> {
                canvas.drawCircle(width / 2, height / 2, width / 2, paint)
                val scale = 1f / (1f + sqrt(2f))
                val borderScale = 0.5f - scale
                val w = width * scale
                val h = height * scale
                val x = width * borderScale
                val y = height * borderScale
                identicons.forEachWithIndex { i, identicon ->
                    identicon.draw(canvas, x + (i % 2) * w, y + (i / 2) * h, w, h)
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        identicons.forEach { it.alpha = alpha }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        identicons.forEach { it.colorFilter = colorFilter }
    }

    override fun getOpacity() = PixelFormat.TRANSPARENT
}
