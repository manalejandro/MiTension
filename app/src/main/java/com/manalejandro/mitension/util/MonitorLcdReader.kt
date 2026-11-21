package com.manalejandro.mitension.util

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Reads seven-segment values from a monitor LCD.
 *
 * On-screen text recognition is very good at the SYS / DIA / PULSE captions but
 * often misses the segmented digits next to them (ML Kit returns the labels of
 * the bundled Omron photo and no readings at all). This reader uses those
 * captions as anchors, then recognises the dark seven-segment glyphs directly
 * from the preview frame. It is deliberately a complement to [MonitorTextParser],
 * not a replacement: displays with normal printed digits are handled by OCR.
 *
 * The result is always presented to the user for confirmation before saving.
 */
object MonitorLcdReader {

    enum class Field { SYS, DIA, PUL }

    data class Label(val field: Field, val box: Rect)

    // Standard seven-segment layouts. Bit order: A B C D E F G.
    private val DIGITS = mapOf(
        "1111110" to '0',
        "0110000" to '1',
        "1101101" to '2',
        "1111001" to '3',
        "0110011" to '4',
        "1011011" to '5',
        "1011111" to '6',
        "1110000" to '7',
        "1111111" to '8',
        "1111011" to '9'
    )

    private data class Component(val rect: Rect, val area: Int) {
        val centerX: Int get() = (rect.left + rect.right) / 2
        val centerY: Double get() = (rect.top + rect.bottom) / 2.0
    }

    /**
     * @param bitmap preview frame, already rotated so that OCR boxes use the
     * same coordinate system
     * @param labels SYS / DIA / PULSE boxes found by text recognition
     */
    fun read(bitmap: Bitmap, labels: List<Label>): ScanValues? {
        val anchors = Field.entries.mapNotNull { field ->
            labels.firstOrNull { it.field == field }?.box
        }
        if (anchors.size != Field.entries.size) return null

        val labelHeight = anchors.sumOf { it.height().toDouble() }.toInt() / anchors.size
        if (labelHeight < 4) return null

        val minLeft = anchors.minOf { it.left }
        val maxRight = anchors.maxOf { it.right }
        val minTop = anchors.minOf { it.top }
        val maxBottom = anchors.maxOf { it.bottom }

        val x0 = max(0, minLeft - labelHeight)
        val y0 = max(0, minTop - labelHeight)
        val x1 = min(bitmap.width, maxRight + labelHeight * 14)
        val y1 = min(bitmap.height, maxBottom + labelHeight * 5)
        val width = x1 - x0
        val height = y1 - y0
        if (width < labelHeight * 8 || height < labelHeight * 8) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, x0, y0, width, height)
        val gray = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (r * 299 + g * 587 + b * 114) / 1000
        }

        val threshold = otsuThreshold(gray)
        if (threshold <= 0 || threshold >= 255) return null
        val ink = BooleanArray(gray.size) { gray[it] < threshold }

        val components = connectedComponents(ink, width, height)
            .map { component ->
                val box = component.rect
                Component(
                    Rect(
                        box.left + x0,
                        box.top + y0,
                        box.right + x0,
                        box.bottom + y0
                    ),
                    component.area
                )
            }
            .filter { component ->
            val box = component.rect
            val boxWidth = box.width()
            val boxHeight = box.height()
            val digitLike = boxHeight >= labelHeight * 0.8 &&
                boxHeight <= labelHeight * 7 &&
                boxWidth <= labelHeight * 5 &&
                component.area >= max(6, labelHeight * labelHeight / 12)
            // Captions are also dark; only look to their right.
            digitLike && component.centerX > maxRight + labelHeight / 5
        }
        if (components.size < 3) return null

        // Captions and digits share the same three rows. Each glyph belongs to
        // the caption whose vertical centre is closest to it.
        val rows = Field.entries.map { mutableListOf<Component>() }
        for (component in components) {
            val nearest = Field.entries.minByOrNull { field ->
                abs(component.centerY - anchors[field.ordinal].verticalCenter())
            } ?: continue
            if (abs(component.centerY - anchors[nearest.ordinal].verticalCenter()) <=
                labelHeight * 3
            ) {
                rows[nearest.ordinal].add(component)
            }
        }

        val values = rows.map { row -> readNumber(ink, width, x0, y0, row) }
        val result = ScanValues(values[0], values[1], values[2])
        return result.takeIf { MonitorTextParser.isValid(it) }
    }

    private fun readNumber(
        ink: BooleanArray,
        width: Int,
        offsetX: Int,
        offsetY: Int,
        components: List<Component>
    ): Double {
        if (components.isEmpty()) return Double.NaN
        val right = components.maxOf { it.rect.right }
        val glyphWidth = components.maxOf { it.rect.width() }
        val pitch = glyphWidth + max(3, glyphWidth / 10)
        val digits = mutableListOf<Char>()

        for (cell in 0 until 4) {
            val left = right - (cell + 1) * pitch
            val cellRight = right - cell * pitch
            val inCell = components.filter { component ->
                val center = component.centerX
                center >= left && center <= cellRight
            }
            if (inCell.isEmpty()) break

            val box = Rect(
                inCell.minOf { it.rect.left },
                inCell.minOf { it.rect.top },
                inCell.maxOf { it.rect.right },
                inCell.maxOf { it.rect.bottom }
            )
            val digit = recogniseDigit(ink, width, offsetX, offsetY, box)
            if (digit != null) digits.add(digit)
        }

        if (digits.isEmpty()) return Double.NaN
        return digits.reversed().joinToString("").toDoubleOrNull() ?: Double.NaN
    }

    private fun recogniseDigit(
        ink: BooleanArray,
        width: Int,
        offsetX: Int,
        offsetY: Int,
        box: Rect
    ): Char? {
        val w = box.width()
        val h = box.height()
        if (w < 2 || h < 4) return null

        // A single vertical stroke is a '1'; all other digits have a horizontal
        // bar that makes them considerably wider than they are tall.
        if (w.toFloat() / h < 0.45f) return '1'

        fun lit(xFrom: Float, xTo: Float, yFrom: Float, yTo: Float): Boolean {
            val left = box.left + (w * xFrom).toInt()
            val right = max(left + 1, box.left + (w * xTo).toInt())
            val top = box.top + (h * yFrom).toInt()
            val bottom = max(top + 1, box.top + (h * yTo).toInt())
            var on = 0
            var total = 0
            for (y in top until bottom) {
                val row = (y - offsetY) * width
                for (x in left until right) {
                    total++
                    if (ink[row + x - offsetX]) on++
                }
            }
            return total > 0 && on.toFloat() / total > 0.25f
        }

        val bits = buildString {
            append(if (lit(0.15f, 0.85f, 0.00f, 0.22f)) '1' else '0') // A
            append(if (lit(0.70f, 1.00f, 0.10f, 0.45f)) '1' else '0') // B
            append(if (lit(0.70f, 1.00f, 0.55f, 0.90f)) '1' else '0') // C
            append(if (lit(0.15f, 0.85f, 0.78f, 1.00f)) '1' else '0') // D
            append(if (lit(0.00f, 0.30f, 0.55f, 0.90f)) '1' else '0') // E
            append(if (lit(0.00f, 0.30f, 0.10f, 0.45f)) '1' else '0') // F
            append(if (lit(0.15f, 0.85f, 0.45f, 0.58f)) '1' else '0') // G
        }

        DIGITS[bits]?.let { return it }
        // Noisy frames may miss one segment: choose the closest digit.
        return DIGITS.minByOrNull { (pattern, _) ->
            pattern.indices.count { bits[it] != pattern[it] }
        }?.value
    }

    private fun otsuThreshold(gray: IntArray): Int {
        val histogram = IntArray(256)
        for (value in gray) histogram[value]++
        val total = gray.size
        var sum = 0
        for (t in 0..255) sum += t * histogram[t]

        var sumBackground = 0
        var weightBackground = 0
        var best = 0
        var bestVariance = -1.0
        for (t in 0..255) {
            weightBackground += histogram[t]
            if (weightBackground == 0) continue
            val weightForeground = total - weightBackground
            if (weightForeground == 0) break
            sumBackground += t * histogram[t]
            val meanBackground = sumBackground.toDouble() / weightBackground
            val meanForeground = (sum - sumBackground).toDouble() / weightForeground
            val variance = weightBackground.toDouble() * weightForeground *
                (meanBackground - meanForeground) * (meanBackground - meanForeground)
            if (variance > bestVariance) {
                bestVariance = variance
                best = t
            }
        }
        return best
    }

    /** Eight-connected dark glyphs, returned in no particular order. */
    private fun connectedComponents(
        ink: BooleanArray,
        width: Int,
        height: Int
    ): List<Component> {
        val visited = BooleanArray(ink.size)
        val stack = IntArray(ink.size)
        val result = mutableListOf<Component>()

        for (start in ink.indices) {
            if (!ink[start] || visited[start]) continue
            var top = 0
            stack[top++] = start
            visited[start] = true
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            var area = 0

            while (top > 0) {
                val position = stack[--top]
                val x = position % width
                val y = position / width
                area++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                for (dy in -1..1) {
                    val ny = y + dy
                    if (ny < 0 || ny >= height) continue
                    for (dx in -1..1) {
                        val nx = x + dx
                        if (nx < 0 || nx >= width) continue
                        val next = ny * width + nx
                        if (ink[next] && !visited[next]) {
                            visited[next] = true
                            stack[top++] = next
                        }
                    }
                }
            }
            if (area > 0) {
                result.add(Component(Rect(minX, minY, maxX, maxY), area))
            }
        }
        return result
    }

    private fun Rect.verticalCenter(): Double = (top + bottom) / 2.0
}
