package com.manalejandro.mitension.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.ui.LocalAppLanguage
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.ui.theme.ChartDiastolic
import com.manalejandro.mitension.ui.theme.ChartPulse
import com.manalejandro.mitension.ui.theme.ChartSystolic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BloodPressureChart(readingsAscending: List<Reading>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
    val language = LocalAppLanguage.current

    Column(modifier = modifier.fillMaxWidth()) {
        val colorScheme = MaterialTheme.colorScheme
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            if (readingsAscending.isEmpty()) return@Canvas

            val series = listOf(
                ChartSeries(ChartSystolic, readingsAscending.map { it.systolic.toFloat() }),
                ChartSeries(ChartDiastolic, readingsAscending.map { it.diastolic.toFloat() }),
                ChartSeries(ChartPulse, readingsAscending.map { it.pulse.toFloat() })
            )

            val allValues = series.flatMap { it.values }
            val rawMin = allValues.minOrNull() ?: return@Canvas
            val rawMax = allValues.maxOrNull() ?: return@Canvas
            val pad = max((rawMax - rawMin) * 0.12f, 8f)
            val yMin = floor(rawMin - pad)
            val yMax = ceil(rawMax + pad)
            val ySpan = (yMax - yMin).coerceAtLeast(1f)

            val leftPad = 44.dp.toPx()
            val bottomPad = 26.dp.toPx()
            val topPad = 14.dp.toPx()
            val rightPad = 16.dp.toPx()
            val plotWidth = size.width - leftPad - rightPad
            val plotHeight = size.height - topPad - bottomPad

            fun y(value: Float): Float =
                topPad + plotHeight * (1f - (value - yMin) / ySpan)

            fun x(index: Int): Float {
                if (readingsAscending.size == 1) return leftPad + plotWidth / 2f
                return leftPad + plotWidth * index / (readingsAscending.size - 1).toFloat()
            }

            val gridColor = colorScheme.outline.copy(alpha = 0.25f)
            for (i in 0..3) {
                val fraction = i / 3f
                val yPos = topPad + plotHeight * (1f - fraction)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, yPos),
                    end = Offset(size.width - rightPad, yPos),
                    strokeWidth = 1.dp.toPx()
                )
                val value = yMin + ySpan * fraction
                drawText(
                    textMeasurer = textMeasurer,
                    text = value.roundToInt().toString(),
                    topLeft = Offset(4.dp.toPx(), yPos - 8.dp.toPx()),
                    style = labelStyle
                )
            }

            val dateFormat = SimpleDateFormat("d MMM", Locale.forLanguageTag(language))
            val labelCount = minOf(4, readingsAscending.size)
            for (i in 0 until labelCount) {
                val index = if (labelCount == 1) 0
                else (readingsAscending.size - 1) * i / (labelCount - 1)
                val reading = readingsAscending[index]
                val text = dateFormat.format(Date(reading.timestamp))
                val textWidth = textMeasurer.measure(text, labelStyle).size.width
                val xPos = x(index) - textWidth / 2f
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(
                        xPos.coerceIn(leftPad, (size.width - rightPad - textWidth).coerceAtLeast(leftPad)),
                        size.height - 20.dp.toPx()
                    ),
                    style = labelStyle
                )
            }

            series.forEach { s ->
                val path = Path()
                readingsAscending.indices.forEach { i ->
                    val point = Offset(x(i), y(s.values[i]))
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            series.forEach { s ->
                readingsAscending.indices.forEach { i ->
                    val point = Offset(x(i), y(s.values[i]))
                    drawCircle(
                        color = colorScheme.background,
                        radius = 5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = s.color,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        ChartLegend()
    }
}

@Composable
private fun ChartLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(ChartSystolic, str(R.string.chart_legend_systolic))
        LegendItem(ChartDiastolic, str(R.string.chart_legend_diastolic))
        LegendItem(ChartPulse, str(R.string.chart_legend_pulse))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ChartSeries(
    val color: Color,
    val values: List<Float>
)