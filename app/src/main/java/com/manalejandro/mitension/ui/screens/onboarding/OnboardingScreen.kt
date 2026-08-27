package com.manalejandro.mitension.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manalejandro.mitension.R
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.BigButton
import com.manalejandro.mitension.ui.components.BigTextField
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.ui.theme.MedicalRed

@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MedicalRed
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = str(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = str(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            AppSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) showError = false
                        },
                        label = str(R.string.onboarding_name_label),
                        placeholder = { Text(str(R.string.onboarding_name_hint)) },
                        isError = showError,
                        supportingText = if (showError) str(R.string.onboarding_error_name) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.height(8.dp))
                    BigButton(
                        text = str(R.string.onboarding_button),
                        onClick = {
                            if (name.isBlank()) {
                                showError = true
                            } else {
                                onComplete(name)
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}