package com.manalejandro.mitension.util

/**
 * Values read from a blood pressure monitor display.
 */
data class ScanValues(
    val systolic: Double,
    val diastolic: Double,
    val pulse: Double
)

/**
 * Turns the raw text recognized by ML Kit on a monitor screen into the three
 * values the app needs. It first looks for the usual SYS / DIA / PUL labels and
 * falls back to plausible numbers in reading order when labels are missing.
 *
 * Results are always meant to be reviewed by the user before being saved.
 */
object MonitorTextParser {

    // Reject partial matches such as the year in "2024" or the first digits of
    // "120.55", which would otherwise become a plausible but wrong reading.
    private val NUMBER = Regex("""(?<!\d)\d{2,3}(?:[.,]\d)?(?!\d|[.,]\d)""")
    private const val LABEL_WINDOW = 48

    private val SYS_LABEL = labelRegex(
        "SYSTOLIC", "SISTOLICA", "SISTÓLICA", "SYST", "SIST", "SYS", "SIS",
        "MAXIMA", "MÁXIMA", "MAX"
    )
    private val DIA_LABEL = labelRegex(
        "DIASTOLIC", "DIASTOLICA", "DIASTÓLICA", "DIAST", "DIA",
        "MINIMA", "MÍNIMA", "MIN"
    )
    private val PUL_LABEL = labelRegex(
        "PULSE", "PULSO", "PULSACIONES", "PULS", "PUL", "HEART", "FRECUENCIA", "FC"
    )

    private fun labelRegex(vararg labels: String): Regex =
        Regex("""(?i)\b(?:${labels.joinToString("|")})\b""")

    fun parse(rawText: String): ScanValues? {
        val text = rawText.trim()
        if (text.isEmpty()) return null

        val hasLabels = SYS_LABEL.containsMatchIn(text) ||
            DIA_LABEL.containsMatchIn(text) ||
            PUL_LABEL.containsMatchIn(text)

        if (!hasLabels) return parseWithoutLabels(text)

        val sysCandidates = labelCandidates(text, SYS_LABEL)
        val diaCandidates = labelCandidates(text, DIA_LABEL)
        val pulCandidates = labelCandidates(text, PUL_LABEL)
        if (sysCandidates.isEmpty() || diaCandidates.isEmpty() || pulCandidates.isEmpty()) {
            return null
        }

        // Monitors place the number either after the label ("SYS 120") or before
        // it ("120 SYS"). Try label-first readings first, then value-first, then
        // mixed layouts, always preferring the closest number to each label.
        val combinations = sysCandidates.flatMap { sys ->
            diaCandidates.flatMap { dia ->
                pulCandidates.map { pul -> Triple(sys, dia, pul) }
            }
        }
            .filter { (sys, dia, pul) ->
                val positions = setOf(sys.position, dia.position, pul.position)
                positions.size == 3
            }
            .sortedWith(
                compareByDescending<Triple<LabelCandidate, LabelCandidate, LabelCandidate>> { combo ->
                    listOf(combo.first.after, combo.second.after, combo.third.after).count { it }
                }.thenBy { combo ->
                    combo.first.distance + combo.second.distance + combo.third.distance
                }
            )

        for ((sys, dia, pul) in combinations) {
            val values = ScanValues(sys.value, dia.value, pul.value)
            if (isValid(values)) return values
        }
        return null
    }

    private data class LabelCandidate(
        val value: Double,
        val position: Int,
        val labelPosition: Int,
        val after: Boolean
    ) {
        val distance: Int get() = kotlin.math.abs(position - labelPosition)
    }

    /** Numbers next to a label, both after it and before it, with positions. */
    private fun labelCandidates(text: String, label: Regex): List<LabelCandidate> =
        buildList {
            for (match in label.findAll(text)) {
                val afterStart = match.range.last + 1
                val afterEnd = minOf(text.length, afterStart + LABEL_WINDOW)
                if (afterStart < afterEnd) {
                    readingNumbers(text, afterStart, afterEnd).firstOrNull()?.let { number ->
                        BloodPressure.parse(number.value)?.let { value ->
                            add(
                                LabelCandidate(
                                    value = value,
                                    position = number.range.first,
                                    labelPosition = match.range.last,
                                    after = true
                                )
                            )
                        }
                    }
                }

                val beforeEnd = match.range.first
                val beforeStart = maxOf(0, beforeEnd - LABEL_WINDOW)
                if (beforeStart < beforeEnd) {
                    readingNumbers(text, beforeStart, beforeEnd).lastOrNull()?.let { number ->
                        BloodPressure.parse(number.value)?.let { value ->
                            add(
                                LabelCandidate(
                                    value = value,
                                    position = number.range.first,
                                    labelPosition = match.range.first,
                                    after = false
                                )
                            )
                        }
                    }
                }
            }
        }.distinctBy { "${it.value}:${it.position}:${it.after}" }

    private data class Candidate(val value: Double, val position: Int)

    // '.' is intentionally absent: a trailing period after a reading ("SYS 128.")
    // is common in OCR output and must not discard the value. Dates written with
    // dots are already rejected by the NUMBER lookarounds.
    private val CLOCK_OR_DATE_SEPARATORS = setOf(':', '/', '-')

    /** Numbers that are not part of a clock time or a date, within [start, end). */
    private fun readingNumbers(text: String, start: Int, end: Int): List<MatchResult> =
        NUMBER.findAll(text, start)
            .takeWhile { it.range.first < end }
            .filter { match ->
                val previous = text.getOrNull(match.range.first - 1)
                val next = text.getOrNull(match.range.last + 1)
                previous !in CLOCK_OR_DATE_SEPARATORS && next !in CLOCK_OR_DATE_SEPARATORS
            }
            .toList()

    /** Ignores numbers that are clearly part of a clock time or a date. */
    private fun candidates(text: String): List<Candidate> =
        readingNumbers(text, 0, text.length).mapNotNull { match ->
            val value = BloodPressure.parse(match.value) ?: return@mapNotNull null
            Candidate(value, match.range.first)
        }

    /**
     * Fallback for displays whose text recognition loses the labels: try the
     * usual SYS-DIA-PUL order first, then any combination that passes the
     * clinical range and relation checks.
     */
    private fun parseWithoutLabels(text: String): ScanValues? {
        val list = candidates(text)
        if (list.size < 3) return null

        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                for (k in j + 1 until list.size) {
                    val values = ScanValues(list[i].value, list[j].value, list[k].value)
                    if (isValid(values)) return values
                }
            }
        }

        for (i in list.indices) {
            for (j in list.indices) {
                if (j == i) continue
                for (k in list.indices) {
                    if (k == i || k == j) continue
                    val values = ScanValues(list[i].value, list[j].value, list[k].value)
                    if (isValid(values)) return values
                }
            }
        }
        return null
    }

    fun isValid(values: ScanValues): Boolean =
        values.systolic in BloodPressure.SYSTOLIC_MIN..BloodPressure.SYSTOLIC_MAX &&
            values.diastolic in BloodPressure.DIASTOLIC_MIN..BloodPressure.DIASTOLIC_MAX &&
            values.pulse in BloodPressure.PULSE_MIN..BloodPressure.PULSE_MAX &&
            values.systolic > values.diastolic
}
