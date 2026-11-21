package com.manalejandro.mitension

import com.manalejandro.mitension.util.MonitorTextParser
import com.manalejandro.mitension.util.ScanValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorTextParserTest {

    @Test
    fun readsLabelledValuesOnSeparateLines() {
        val text = "SYS\n128\nDIA\n82\nPUL\n67"
        assertEquals(ScanValues(128.0, 82.0, 67.0), MonitorTextParser.parse(text))
    }

    @Test
    fun readsLabelledValuesOnOneLine() {
        val text = "SYS 118  DIA 76  PUL 64"
        assertEquals(ScanValues(118.0, 76.0, 64.0), MonitorTextParser.parse(text))
    }

    @Test
    fun ignoresTimeAndDateNoise() {
        val text = "12:45  21/11  SYS 134  DIA 86  PUL 72"
        assertEquals(ScanValues(134.0, 86.0, 72.0), MonitorTextParser.parse(text))
    }

    @Test
    fun acceptsDecimalComma() {
        val text = "SYS 127,5\nDIA 84,5\nPUL 71"
        assertEquals(ScanValues(127.5, 84.5, 71.0), MonitorTextParser.parse(text))
    }

    @Test
    fun acceptsValuesBeforeLabels() {
        val text = "122 SYS\n79 DIA\n68 PUL"
        assertEquals(ScanValues(122.0, 79.0, 68.0), MonitorTextParser.parse(text))
    }

    @Test
    fun fallsBackToNumbersInReadingOrder() {
        val text = "OK 145 92 88 mmHg"
        assertEquals(ScanValues(145.0, 92.0, 88.0), MonitorTextParser.parse(text))
    }

    @Test
    fun rejectsImpossibleValues() {
        assertNull(MonitorTextParser.parse("SYS 380 DIA 92 PUL 70"))
        assertNull(MonitorTextParser.parse("SYS 120 DIA 150 PUL 70"))
        assertNull(MonitorTextParser.parse("SYS 120 DIA 80 PUL 250"))
    }

    @Test
    fun rejectsEmptyOrIncompleteText() {
        assertNull(MonitorTextParser.parse(""))
        assertNull(MonitorTextParser.parse("SYS 120 DIA 80"))
    }

    @Test
    fun readsSpanishLabelsWithAccents() {
        val text = "MÁXIMA 128  MÍNIMA 82  PULSO 67"
        assertEquals(ScanValues(128.0, 82.0, 67.0), MonitorTextParser.parse(text))
    }

    @Test
    fun ignoresFullDatesWithoutLabels() {
        val text = "2024-11-21  145  92  88"
        assertEquals(ScanValues(145.0, 92.0, 88.0), MonitorTextParser.parse(text))
    }

    @Test
    fun doesNotSplitNumbersWithTwoDecimals() {
        assertNull(MonitorTextParser.parse("SYS 120.55 DIA 80 PUL 70"))
    }

    @Test
    fun acceptsTrailingPunctuationAfterValues() {
        val text = "SYS 128. DIA 82. PUL 67."
        assertEquals(ScanValues(128.0, 82.0, 67.0), MonitorTextParser.parse(text))
    }

    @Test
    fun skipsDateBetweenLabelAndValue() {
        val text = "SYS 21/11 128 DIA 82 PUL 67"
        assertEquals(ScanValues(128.0, 82.0, 67.0), MonitorTextParser.parse(text))
    }

    @Test
    fun readsRealisticOmronScreen() {
        val text = "OMRON\nSYS mmHg\n128\nDIA mmHg\n82\nPUL /min\n67\n12:45"
        assertEquals(ScanValues(128.0, 82.0, 67.0), MonitorTextParser.parse(text))
    }
}
