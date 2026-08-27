package com.manalejandro.mitension.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.ui.screens.history.HistoryScreen
import com.manalejandro.mitension.ui.screens.home.HomeScreen
import com.manalejandro.mitension.ui.screens.reminders.RemindersScreen
import com.manalejandro.mitension.ui.screens.settings.SettingsScreen
import com.manalejandro.mitension.ui.screens.users.UsersScreen

private enum class AppTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    NEW(R.string.tab_new, Icons.Filled.MonitorHeart),
    HISTORY(R.string.tab_history, Icons.Filled.History),
    REMINDERS(R.string.tab_reminders, Icons.Filled.Alarm),
    USERS(R.string.tab_users, Icons.Filled.Group),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings)
}

@Composable
fun MainScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingReading by remember { mutableStateOf<Reading?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index != AppTab.NEW.ordinal) editingReading = null
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                str(tab.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            AppTab.NEW.ordinal -> HomeScreen(
                vm = vm,
                editingReading = editingReading,
                onClearEditing = { editingReading = null },
                onGoToUsers = { selectedTab = AppTab.USERS.ordinal },
                snackbarHostState = snackbarHostState,
                modifier = contentModifier
            )
            AppTab.HISTORY.ordinal -> HistoryScreen(
                vm = vm,
                onEditReading = { reading ->
                    editingReading = reading
                    selectedTab = AppTab.NEW.ordinal
                },
                modifier = contentModifier
            )
            AppTab.REMINDERS.ordinal -> RemindersScreen(vm = vm, modifier = contentModifier)
            AppTab.USERS.ordinal -> UsersScreen(vm = vm, modifier = contentModifier)
            AppTab.SETTINGS.ordinal -> SettingsScreen(vm = vm, modifier = contentModifier)
        }
    }
}