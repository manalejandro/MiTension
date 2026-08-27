package com.manalejandro.mitension.util

import java.util.Locale
import kotlin.math.floor

object BloodPressure {

    const val SYSTOLIC_MIN = 60.0
    const val SYSTOLIC_MAX = 260.0
    const val DIASTOLIC_MIN = 30.0
    const val DIASTOLIC_MAX = 200.0
    const val PULSE_MIN = 30.0
    const val PULSE_MAX = 220.0

    enum class Category { LOW, NORMAL, ELEVATED, HYPERTENSION_1, HYPERTENSION_2 }

    data class ValidationResult(
        val systolicValid: Boolean = false,
        val diastolicValid: Boolean = false,
        val pulseValid: Boolean = false,
        val relationValid: Boolean = true
    ) {
        val isValid: Boolean
            get() = systolicValid && diastolicValid && pulseValid && relationValid
    }

    /** Parses a pressure/pulse string accepting both '.' and ',' as decimal separator. */
    fun parse(raw: String): Double? = raw.trim().replace(',', '.').toDoubleOrNull()

    fun validate(systolic: String, diastolic: String, pulse: String): ValidationResult {
        val sys = parse(systolic)
        val dia = parse(diastolic)
        val pul = parse(pulse)
        return ValidationResult(
            systolicValid = sys != null && sys in SYSTOLIC_MIN..SYSTOLIC_MAX,
            diastolicValid = dia != null && dia in DIASTOLIC_MIN..DIASTOLIC_MAX,
            pulseValid = pul != null && pul in PULSE_MIN..PULSE_MAX,
            relationValid = sys != null && dia != null && sys > dia
        )
    }

    fun category(systolic: Double, diastolic: Double): Category = when {
        systolic >= 140.0 || diastolic >= 90.0 -> Category.HYPERTENSION_2
        systolic >= 130.0 || diastolic >= 80.0 -> Category.HYPERTENSION_1
        systolic >= 120.0 && diastolic < 80.0 -> Category.ELEVATED
        systolic < 90.0 || diastolic < 60.0 -> Category.LOW
        else -> Category.NORMAL
    }

    /** Formats a value for display: integers without decimals, otherwise one decimal. */
    fun formatValue(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == floor(rounded)) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    /**
     * Sanitizes a numeric field while typing: digits plus a single decimal
     * separator ('.' or ','), with limited integer and decimal digits.
     */
    fun sanitizeDecimal(raw: String, maxIntDigits: Int = 3, maxDecimals: Int = 1): String {
        var text = raw.filter { it.isDigit() || it == '.' || it == ',' }
        if (text.isEmpty()) return ""

        val dotIndex = text.indexOf('.')
        val commaIndex = text.indexOf(',')
        val sep: Char = when {
            commaIndex == -1 -> '.'
            dotIndex == -1 -> ','
            commaIndex < dotIndex -> ','
            else -> '.'
        }
        val other = if (sep == '.') ',' else '.'
        text = text.filter { it != other }
        val idx = text.indexOf(sep)
        if (idx >= 0) {
            val intPart = text.substring(0, idx).take(maxIntDigits)
            val decPart = text.substring(idx + 1).filter { it != sep }.take(maxDecimals)
            return if (decPart.isEmpty()) "$intPart$sep" else "$intPart$sep$decPart"
        }
        return text.take(maxIntDigits)
    }
}