package com.manalejandro.mitension.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.ui.LocalAppLanguage
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.BigButton
import com.manalejandro.mitension.ui.components.BigOutlinedButton
import com.manalejandro.mitension.ui.components.BigTextField
import com.manalejandro.mitension.ui.components.CategoryBadge
import com.manalejandro.mitension.ui.components.categoryColor
import com.manalejandro.mitension.ui.components.categoryLabelRes
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.util.BloodPressure
import com.manalejandro.mitension.util.ScanValues
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    vm: MainViewModel,
    editingReading: Reading?,
    onClearEditing: () -> Unit,
    onGoToUsers: () -> Unit,
    onOpenScan: () -> Unit,
    scanValues: ScanValues?,
    onScanValuesConsumed: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val activeUser by vm.activeUser.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()

    var sysText by remember { mutableStateOf("") }
    var diaText by remember { mutableStateOf("") }
    var pulseText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(editingReading?.id) {
        val reading = editingReading
        if (reading == null) {
            sysText = ""
            diaText = ""
            pulseText = ""
            note = ""
        } else {
            sysText = BloodPressure.formatValue(reading.systolic)
            diaText = BloodPressure.formatValue(reading.diastolic)
            pulseText = BloodPressure.formatValue(reading.pulse)
            note = reading.note
        }
    }

    val scope = rememberCoroutineScope()
    val scanPrefilledMessage = str(R.string.scan_values_prefilled)
    LaunchedEffect(scanValues) {
        if (scanValues != null) {
            sysText = BloodPressure.formatValue(scanValues.systolic)
            diaText = BloodPressure.formatValue(scanValues.diastolic)
            pulseText = BloodPressure.formatValue(scanValues.pulse)
            onScanValuesConsumed()
            // Show it from the composition scope so clearing the pending values
            // (which cancels this effect) does not dismiss the message.
            scope.launch { snackbarHostState.showSnackbar(scanPrefilledMessage) }
        }
    }

    val validation = BloodPressure.validate(sysText, diaText, pulseText)

    val lastReading = readings.firstOrNull()
    val language = LocalAppLanguage.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${str(R.string.home_hello)}, ${activeUser?.name ?: ""}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = str(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activeUser != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            str(R.string.home_active_user),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            activeUser?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onGoToUsers) {
                        Text(
                            str(R.string.home_switch_user),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (lastReading != null) {
            AppSectionCard(title = str(R.string.home_last_reading)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${BloodPressure.formatValue(lastReading.systolic)}/" +
                            BloodPressure.formatValue(lastReading.diastolic),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${str(R.string.home_pulse)}: ${BloodPressure.formatValue(lastReading.pulse)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateTime(lastReading.timestamp, language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    CategoryBadge(
                        text = categoryLabel(lastReading.systolic, lastReading.diastolic),
                        color = categoryColor(lastReading.systolic, lastReading.diastolic)
                    )
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BigOutlinedButton(
                    text = str(R.string.scan_open),
                    onClick = onOpenScan,
                    icon = Icons.Filled.CameraAlt
                )
                Text(
                    text = str(R.string.scan_open_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        AppSectionCard(title = if (editingReading == null) str(R.string.home_new_title) else str(R.string.home_edit_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BigTextField(
                    value = sysText,
                    onValueChange = { sysText = BloodPressure.sanitizeDecimal(it) },
                    label = str(R.string.home_systolic),
                    leadingIcon = Icons.Filled.Speed,
                    trailingText = str(R.string.home_systolic_unit),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = sysText.isNotBlank() && !validation.systolicValid,
                    supportingText = if (sysText.isNotBlank() && !validation.systolicValid) str(R.string.err_systolic_range) else null
                )
                BigTextField(
                    value = diaText,
                    onValueChange = { diaText = BloodPressure.sanitizeDecimal(it) },
                    label = str(R.string.home_diastolic),
                    leadingIcon = Icons.Filled.WaterDrop,
                    trailingText = str(R.string.home_systolic_unit),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = diaText.isNotBlank() && !validation.diastolicValid,
                    supportingText = if (diaText.isNotBlank() && !validation.diastolicValid) str(R.string.err_diastolic_range) else null
                )
                BigTextField(
                    value = pulseText,
                    onValueChange = { pulseText = BloodPressure.sanitizeDecimal(it) },
                    label = str(R.string.home_pulse),
                    leadingIcon = Icons.Filled.MonitorHeart,
                    trailingText = str(R.string.home_pulse_unit),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = pulseText.isNotBlank() && !validation.pulseValid,
                    supportingText = if (pulseText.isNotBlank() && !validation.pulseValid) str(R.string.err_pulse_range) else null
                )
                if (sysText.isNotBlank() && diaText.isNotBlank() && !validation.relationValid) {
                    Text(
                        text = str(R.string.err_sys_gt_dia),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                val sys = BloodPressure.parse(sysText)
                val dia = BloodPressure.parse(diaText)
                if (sys != null && dia != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            str(R.string.home_category),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CategoryBadge(
                            text = categoryLabel(sys, dia),
                            color = categoryColor(sys, dia)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                BigTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = str(R.string.home_note),
                    placeholder = { Text(str(R.string.home_note_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLength = 200
                )

                Spacer(Modifier.height(8.dp))
                val pulse = BloodPressure.parse(pulseText)
                val valid = validation.isValid && sys != null && dia != null && pulse != null
                val editMessage = str(R.string.home_reading_updated)
                val saveMessage = str(R.string.home_reading_saved)

                if (editingReading != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BigOutlinedButton(
                            text = str(R.string.home_cancel),
                            onClick = onClearEditing,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Edit
                        )
                        BigButton(
                            text = str(R.string.home_save_edit),
                            onClick = {
                                if (valid) {
                                    scope.launch {
                                        vm.updateReading(
                                            editingReading.copy(
                                                systolic = sys!!,
                                                diastolic = dia!!,
                                                pulse = pulse!!,
                                                note = note
                                            )
                                        )
                                        snackbarHostState.showSnackbar(editMessage)
                                    }
                                    onClearEditing()
                                }
                            },
                            enabled = valid,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                } else {
                    BigButton(
                        text = str(R.string.home_save),
                        onClick = {
                            if (valid) {
                                scope.launch {
                                    vm.addReading(
                                        systolic = sys!!,
                                        diastolic = dia!!,
                                        pulse = pulse!!,
                                        note = note
                                    )
                                    snackbarHostState.showSnackbar(saveMessage)
                                }
                                sysText = ""
                                diaText = ""
                                pulseText = ""
                                note = ""
                            }
                        },
                        enabled = valid,
                        icon = Icons.Filled.Favorite
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun categoryLabel(systolic: Double, diastolic: Double): String =
    str(categoryLabelRes(BloodPressure.category(systolic, diastolic)))

internal fun formatDateTime(timestamp: Long, language: String): String {
    val locale = java.util.Locale.forLanguageTag(language)
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(timestamp))
}