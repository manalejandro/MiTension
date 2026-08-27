package com.manalejandro.mitension.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.ui.LocalAppLanguage
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.chart.BloodPressureChart
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.EmptyState
import com.manalejandro.mitension.ui.components.categoryColor
import com.manalejandro.mitension.ui.screens.home.formatDateTime
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.util.BloodPressure

@Composable
fun HistoryScreen(
    vm: MainViewModel,
    onEditReading: (Reading) -> Unit,
    modifier: Modifier = Modifier
) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    var readingToDelete by remember { mutableStateOf<Reading?>(null) }

    val readingsAscending = readings.asReversed()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = str(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (readings.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                text = str(R.string.history_empty),
                modifier = Modifier.padding(top = 48.dp)
            )
            return@Column
        }

        StatsRow(readings = readings)

        Spacer(Modifier.height(20.dp))

        AppSectionCard(title = null) {
            BloodPressureChart(readingsAscending = readingsAscending)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = str(R.string.history_readings_count, readings.size),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(readings, key = { it.id }) { reading ->
                ReadingCard(
                    reading = reading,
                    language = language,
                    onEdit = { onEditReading(reading) },
                    onDelete = { readingToDelete = reading }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
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
                fontWeight = FontWeight.Bold
            )
            Text(
                "${str(R.string.home_pulse)}: $sub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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