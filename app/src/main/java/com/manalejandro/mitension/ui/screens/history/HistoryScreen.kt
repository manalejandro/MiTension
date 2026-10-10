package com.manalejandro.mitension.ui.screens.history

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.ui.LocalAppLanguage
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.chart.BloodPressureChart
import com.manalejandro.mitension.ui.chart.PulseChart
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.EmptyState
import com.manalejandro.mitension.ui.components.categoryColor
import com.manalejandro.mitension.ui.components.categoryLabelRes
import com.manalejandro.mitension.ui.screens.home.formatDateTime
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.ui.theme.ChartDiastolic
import com.manalejandro.mitension.ui.theme.ChartSystolic
import com.manalejandro.mitension.util.BloodPressure
import java.util.Calendar

private enum class HistoryRange(@StringRes val labelRes: Int) {
    WEEK(R.string.chart_filter_7d),
    MONTH(R.string.chart_filter_30d),
    ALL(R.string.chart_filter_all);

    fun cutoffMillis(): Long? = when (this) {
        WEEK -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        MONTH -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        ALL -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: MainViewModel,
    onEditReading: (Reading) -> Unit,
    modifier: Modifier = Modifier
) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    var range by rememberSaveable { mutableStateOf(HistoryRange.ALL) }
    var readingToDelete by remember { mutableStateOf<Reading?>(null) }

    val filtered = remember(readings, range) {
        val cutoff = range.cutoffMillis()
        if (cutoff == null) readings else readings.filter { it.timestamp >= cutoff }
    }
    val readingsAscending = remember(filtered) { filtered.asReversed() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = str(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (readings.isNotEmpty()) {
            item {
                RangeSelector(selected = range, onSelect = { range = it })
            }
        }

        if (readings.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    text = str(R.string.history_empty),
                    modifier = Modifier.padding(top = 48.dp)
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    text = str(R.string.chart_no_data_period),
                    modifier = Modifier.padding(top = 48.dp)
                )
            }
        } else {
            item { StatsRow(readings = filtered) }

            item {
                AppSectionCard(title = str(R.string.chart_trend_title)) {
                    BloodPressureChart(readingsAscending = readingsAscending)
                }
            }

            item {
                AppSectionCard(title = str(R.string.chart_pulse_title)) {
                    PulseChart(readingsAscending = readingsAscending)
                }
            }

            item { SlotAveragesCard(readings = filtered) }

            item { CategoryDistributionCard(readings = filtered) }

            item {
                Text(
                    text = str(R.string.history_readings_count, filtered.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filtered, key = { it.id }) { reading ->
                ReadingCard(
                    reading = reading,
                    language = language,
                    onEdit = { onEditReading(reading) },
                    onDelete = { readingToDelete = reading }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    readingToDelete?.let { reading ->
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = { Text(str(R.string.history_delete_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(str(R.string.history_delete_message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteReading(reading)
                        readingToDelete = null
                    }
                ) {
                    Text(
                        str(R.string.history_confirm_delete),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { readingToDelete = null }) {
                    Text(str(R.string.history_cancel), style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryRange.entries.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = {
                    Text(
                        str(item.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun StatsRow(readings: List<Reading>) {
    val sys = readings.map { it.systolic }
    val dia = readings.map { it.diastolic }
    val pulse = readings.map { it.pulse }
    val last = readings.first()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = str(R.string.history_last),
            value = "${BloodPressure.formatValue(last.systolic)}/${BloodPressure.formatValue(last.diastolic)}",
            sub = BloodPressure.formatValue(last.pulse),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = str(R.string.history_average),
            value = "${BloodPressure.formatValue(sys.average())}/${BloodPressure.formatValue(dia.average())}",
            sub = BloodPressure.formatValue(pulse.average()),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${str(R.string.home_pulse)}: $sub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun slotIndex(hourOfDay: Int): Int = when (hourOfDay) {
    in 6..11 -> 0
    in 12..16 -> 1
    in 17..21 -> 2
    else -> 3
}

@StringRes
private fun slotLabelRes(slot: Int): Int = when (slot) {
    0 -> R.string.chart_slot_morning
    1 -> R.string.chart_slot_midday
    2 -> R.string.chart_slot_evening
    else -> R.string.chart_slot_night
}

private class SlotAccumulator(
    var sumSystolic: Double = 0.0,
    var sumDiastolic: Double = 0.0,
    var count: Int = 0
)

@Composable
private fun SlotAveragesCard(readings: List<Reading>) {
    val accumulators = remember(readings) {
        val calendar = Calendar.getInstance()
        val list = List(4) { SlotAccumulator() }
        readings.forEach { reading ->
            calendar.timeInMillis = reading.timestamp
            val slot = slotIndex(calendar.get(Calendar.HOUR_OF_DAY))
            val acc = list[slot]
            acc.sumSystolic += reading.systolic
            acc.sumDiastolic += reading.diastolic
            acc.count += 1
        }
        list
    }
    val visibleSlots = accumulators.indices.filter { accumulators[it].count > 0 }
    if (visibleSlots.isEmpty()) return

    val averages = visibleSlots.map { index ->
        val acc = accumulators[index]
        Triple(acc.sumSystolic / acc.count, acc.sumDiastolic / acc.count, acc.count)
    }
    val scaleMax = averages
        .flatMap { listOf(it.first, it.second) }
        .maxOrNull()
        ?.coerceAtLeast(140.0)
        ?.toFloat() ?: 140f

    AppSectionCard(title = str(R.string.chart_by_slot_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            visibleSlots.forEachIndexed { position, slot ->
                val (avgSys, avgDia, count) = averages[position]
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = str(slotLabelRes(slot)),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = str(R.string.chart_slot_readings, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${BloodPressure.formatValue(avgSys)}/" +
                                BloodPressure.formatValue(avgDia),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    MetricBar(fraction = (avgSys / scaleMax).toFloat(), color = ChartSystolic)
                    Spacer(Modifier.height(4.dp))
                    MetricBar(fraction = (avgDia / scaleMax).toFloat(), color = ChartDiastolic)
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionCard(readings: List<Reading>) {
    val counts = remember(readings) {
        readings.groupingBy { BloodPressure.category(it.systolic, it.diastolic) }.eachCount()
    }
    val total = readings.size
    if (total == 0) return

    AppSectionCard(title = str(R.string.chart_categories_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BloodPressure.Category.entries.forEach { category ->
                val count = counts[category] ?: 0
                if (count > 0) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = str(categoryLabelRes(category)),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor(category)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        MetricBar(
                            fraction = count.toFloat() / total.toFloat(),
                            color = categoryColor(category)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ReadingCard(
    reading: Reading,
    language: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = BloodPressure.category(reading.systolic, reading.diastolic)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(14.dp),
                        shape = CircleShape,
                        color = categoryColor(category)
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${BloodPressure.formatValue(reading.systolic)}/" +
                            BloodPressure.formatValue(reading.diastolic),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = BloodPressure.formatValue(reading.pulse),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatDateTime(reading.timestamp, language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reading.note.isNotBlank()) {
                    Text(
                        text = reading.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = str(R.string.history_edit),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = str(R.string.history_delete),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}