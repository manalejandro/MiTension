package com.manalejandro.mitension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.manalejandro.mitension.util.MonitorLcdReader
import com.manalejandro.mitension.util.MonitorTextParser
import com.manalejandro.mitension.util.ScanValues
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic and regression test: runs the real ML Kit recognizer on the bundled
 * Omron guide photo and on simulated camera frames, prints what it sees and
 * checks that the LCD reader recovers the expected values at several scales.
 */
@RunWith(AndroidJUnit4::class)
class OcrProbeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @Test
    fun probeGuideImage() {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.omron_guide)
        requireNotNull(source) { "guide image not found" }
        probe("original ${source.width}x${source.height}", source)

        probe("frame 640x480", frame(source, 640, 480))
        probe("frame 1280x720", frame(source, 1280, 720))
        probe("frame 1280x720 preprocessed", preprocess(frame(source, 1280, 720)))
        probe("guide preprocessed 2x", preprocess(scale(source, 2f)))

        val expected = ScanValues(118.0, 78.0, 70.0)
        assertEquals(expected, readLcd("guide", source))
        assertEquals(expected, readLcd("frame 640x480", frame(source, 640, 480)))
        assertEquals(expected, readLcd("frame 1280x720", frame(source, 1280, 720)))
        assertEquals(expected, readLcd("guide 2x", scale(source, 2f)))
    }

    private fun readLcd(label: String, bitmap: Bitmap): ScanValues? {
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
        val elements = text.textBlocks.flatMap { it.lines }.flatMap { it.elements }
        val fields = mapOf(
            MonitorLcdReader.Field.SYS to Regex("(?i)^(SYS|SYST|SIST|SIS)$"),
            MonitorLcdReader.Field.DIA to Regex("(?i)^(DIA|DIAST)$"),
            MonitorLcdReader.Field.PUL to Regex("(?i)^(PUL|PULS|PULSE|PULSO)$")
        )
        val boxes = fields.mapNotNull { (field, regex) ->
            val box = elements.firstOrNull { regex.matches(it.text) }?.boundingBox
            box?.let { MonitorLcdReader.Label(field, it) }
        }
        val values = MonitorLcdReader.read(bitmap, boxes)
        Log.i(TAG, "LCD $label: labels=$boxes values=$values")
        return values
    }

    private fun frame(source: Bitmap, width: Int, height: Int): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.rgb(35, 35, 35))
        val side = (minOf(width, height) * 0.8f).toInt()
        val scaled = Bitmap.createScaledBitmap(source, side, side, true)
        canvas.drawBitmap(
            scaled,
            (width - side) / 2f,
            (height - side) / 2f,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return out
    }

    private fun scale(source: Bitmap, factor: Float): Bitmap =
        Bitmap.createScaledBitmap(
            source,
            (source.width * factor).toInt(),
            (source.height * factor).toInt(),
            true
        )

    /**
     * Grayscale + contrast stretch + mild upscale, the same idea the scanner
     * would apply before recognition. Returns a bitmap of the same size.
     */
    private fun preprocess(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(pixels.size)
        var min = 255
        var max = 0
        for (i in pixels.indices) {
            val p = pixels[i]
            val l = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)).toInt()
            gray[i] = l
            if (l < min) min = l
            if (l > max) max = l
        }
        val range = (max - min).coerceAtLeast(1)
        for (i in gray.indices) {
            val v = ((gray[i] - min) * 255 / range).coerceIn(0, 255)
            gray[i] = Color.rgb(v, v, v)
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(gray, 0, width, 0, 0, width, height)
        return out
    }

    private fun probe(label: String, bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = Tasks.await(recognizer.process(image))
        val raw = text.text
        Log.i(TAG, "=== $label ===")
        Log.i(TAG, "raw: ${raw.replace("\n", "\\n")}")
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                Log.i(
                    TAG,
                    "line '${line.text}' box=${box?.left},${box?.top},${box?.right},${box?.bottom}"
                )
            }
        }
        Log.i(TAG, "parsed: ${MonitorTextParser.parse(raw)}")
    }

    private companion object {
        const val TAG = "OCR_PROBE"
    }
}
