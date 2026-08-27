package com.manalejandro.mitension

import com.manalejandro.mitension.util.BloodPressure
import com.manalejandro.mitension.util.BloodPressure.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodPressureTest {

    @Test
    fun validReadingIsValid() {
        val result = BloodPressure.validate("120", "80", "72")
        assertTrue(result.isValid)
        assertTrue(result.systolicValid)
        assertTrue(result.diastolicValid)
        assertTrue(result.pulseValid)
        assertTrue(result.relationValid)
    }

    @Test
    fun decimalValuesAreValid() {
        val result = BloodPressure.validate("120.5", "79.5", "72")
        assertTrue(result.isValid)
        assertEquals(120.5, BloodPressure.parse("120.5")!!, 0.001)
    }

    @Test
    fun commaDecimalSeparatorIsAccepted() {
        val result = BloodPressure.validate("120,5", "79,5", "72,5")
        assertTrue(result.isValid)
        assertEquals(120.5, BloodPressure.parse("120,5")!!, 0.001)
    }

    @Test
    fun nonNumericInputIsRejected() {
        val result = BloodPressure.validate("abc", "80", "72")
        assertFalse(result.systolicValid)
        assertFalse(result.isValid)
    }

    @Test
    fun outOfRangeValuesAreRejected() {
        assertFalse(BloodPressure.validate("300", "80", "72").systolicValid)
        assertFalse(BloodPressure.validate("120", "250", "72").diastolicValid)
        assertFalse(BloodPressure.validate("120", "80", "400").pulseValid)
        assertFalse(BloodPressure.validate("59.9", "80", "72").systolicValid)
    }

    @Test
    fun systolicMustExceedDiastolic() {
        val result = BloodPressure.validate("80", "120", "70")
        assertFalse(result.relationValid)
        assertFalse(result.isValid)
        val decimal = BloodPressure.validate("120.5", "120.5", "70")
        assertFalse(decimal.relationValid)
    }

    @Test
    fun emptyFieldsAreInvalid() {
        val result = BloodPressure.validate("", "", "")
        assertFalse(result.isValid)
    }

    @Test
    fun categoryNormal() {
        assertEquals(Category.NORMAL, BloodPressure.category(110.0, 70.0))
    }

    @Test
    fun categoryLow() {
        assertEquals(Category.LOW, BloodPressure.category(85.0, 55.0))
        assertEquals(Category.LOW, BloodPressure.category(89.9, 55.0))
    }

    @Test
    fun categoryElevated() {
        assertEquals(Category.ELEVATED, BloodPressure.category(125.0, 75.0))
    }

    @Test
    fun categoryHypertension1() {
        assertEquals(Category.HYPERTENSION_1, BloodPressure.category(135.0, 85.0))
        assertEquals(Category.HYPERTENSION_1, BloodPressure.category(130.0, 79.9))
    }

    @Test
    fun categoryHypertension2() {
        assertEquals(Category.HYPERTENSION_2, BloodPressure.category(150.0, 95.0))
        assertEquals(Category.HYPERTENSION_2, BloodPressure.category(139.9, 90.0))
    }

    @Test
    fun formatValue() {
        assertEquals("120", BloodPressure.formatValue(120.0))
        assertEquals("120.5", BloodPressure.formatValue(120.5))
        assertEquals("80.5", BloodPressure.formatValue(80.54))
    }

    @Test
    fun sanitizeDecimalAllowsOneSeparator() {
        assertEquals("120.5", BloodPressure.sanitizeDecimal("120.5"))
        assertEquals("120,5", BloodPressure.sanitizeDecimal("120,5"))
        assertEquals("120.5", BloodPressure.sanitizeDecimal("120..5"))
        assertEquals("120.5", BloodPressure.sanitizeDecimal("12a0.5x"))
        assertEquals("123", BloodPressure.sanitizeDecimal("12345"))
        assertEquals("1.5", BloodPressure.sanitizeDecimal("1.55"))
    }
}