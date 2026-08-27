package com.manalejandro.mitension.ui.screens.users

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.User
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.components.BigButton
import com.manalejandro.mitension.ui.components.BigOutlinedButton
import com.manalejandro.mitension.ui.components.BigTextField
import com.manalejandro.mitension.ui.components.EmptyState
import com.manalejandro.mitension.ui.str

@Composable
fun UsersScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val users by vm.users.collectAsStateWithLifecycle()
    val activeUser by vm.activeUser.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var userToRename by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = str(R.string.users_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        BigButton(
            text = str(R.string.users_add),
            onClick = { showAddDialog = true },
            icon = Icons.Filled.Add
        )
        Spacer(Modifier.height(16.dp))

        if (users.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Group,
                text = str(R.string.users_empty),
                modifier = Modifier.padding(top = 48.dp)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    userCount = users.size,
                    isActive = user.id == activeUser?.id,
                    onSwitch = { vm.setActiveUser(user.id) },
                    onRename = { userToRename = user },
                    onDelete = { userToDelete = user }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = str(R.string.users_new_title),
            confirmText = str(R.string.users_save),
            onConfirm = { name ->
                vm.createUser(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    userToRename?.let { user ->
        NameDialog(
            title = str(R.string.users_rename_title),
            confirmText = str(R.string.users_save),
            initialName = user.name,
            onConfirm = { name ->
                vm.renameUser(user, name)
                userToRename = null
            },
            onDismiss = { userToRename = null }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(str(R.string.users_delete_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(str(R.string.users_delete_message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteUser(user)
                        userToDelete = null
                    }
                ) {
                    Text(
                        str(R.string.users_delete),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(str(R.string.users_cancel), style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    userCount: Int,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (user.isPrimary) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = str(R.string.users_primary),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            str(R.string.users_active),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!isActive) {
                TextButton(onClick = onSwitch) {
                    Text(
                        str(R.string.users_switch),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onRename) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = str(R.string.users_edit),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = !user.isPrimary && userCount > 1
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = str(R.string.users_delete),
                    modifier = Modifier.size(28.dp),
                    tint = if (user.isPrimary || userCount <= 1) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = ""
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                BigTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) showError = false
                    },
                    label = str(R.string.users_name_label),
                    placeholder = { Text(str(R.string.users_name_hint)) },
                    isError = showError,
                    supportingText = if (showError) str(R.string.onboarding_error_name) else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(name)
                    }
                }
            ) {
                Text(confirmText, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str(R.string.users_cancel), style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}