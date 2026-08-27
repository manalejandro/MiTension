package com.manalejandro.mitension.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.TEXT_SCALE_LARGE
import com.manalejandro.mitension.data.TEXT_SCALE_NORMAL
import com.manalejandro.mitension.data.TEXT_SCALE_XLARGE
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.BigOutlinedButton
import com.manalejandro.mitension.ui.str

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val language = settings.language ?: com.manalejandro.mitension.util.AppLocale.systemLanguage()
    val textScale = settings.textScale

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = str(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        AppSectionCard(title = str(R.string.settings_language)) {
            Column {
                SettingOptionRow(
                    label = str(R.string.settings_language_es),
                    selected = language == "es",
                    onClick = { vm.setLanguage("es") }
                )
                Spacer(Modifier.height(8.dp))
                SettingOptionRow(
                    label = str(R.string.settings_language_en),
                    selected = language == "en",
                    onClick = { vm.setLanguage("en") }
                )
            }
        }

        AppSectionCard(title = str(R.string.settings_text_size)) {
            Column {
                SettingOptionRow(
                    label = str(R.string.settings_text_normal),
                    selected = textScale == TEXT_SCALE_NORMAL,
                    onClick = { vm.setTextScale(TEXT_SCALE_NORMAL) }
                )
                Spacer(Modifier.height(8.dp))
                SettingOptionRow(
                    label = str(R.string.settings_text_large),
                    selected = textScale == TEXT_SCALE_LARGE,
                    onClick = { vm.setTextScale(TEXT_SCALE_LARGE) }
                )
                Spacer(Modifier.height(8.dp))
                SettingOptionRow(
                    label = str(R.string.settings_text_xlarge),
                    selected = textScale == TEXT_SCALE_XLARGE,
                    onClick = { vm.setTextScale(TEXT_SCALE_XLARGE) }
                )
            }
        }

        AppSectionCard(title = str(R.string.settings_notifications)) {
            Column {
                Text(
                    text = str(R.string.reminder_perm_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                BigOutlinedButton(
                    text = str(R.string.settings_open_system),
                    icon = Icons.Filled.Notifications,
                    onClick = { openNotificationSettings(context) }
                )
            }
        }

        AppSectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${str(R.string.settings_about)} · ${str(R.string.settings_version)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = str(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }
    context.startActivity(intent)
}