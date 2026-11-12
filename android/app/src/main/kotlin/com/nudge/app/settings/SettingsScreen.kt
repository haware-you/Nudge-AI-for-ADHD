package com.nudge.app.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.nudge.app.R
import com.nudge.app.ui.components.NudgeCard
import com.nudge.app.ui.components.ScreenHeader
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

private data class SettingItem(val title: String, val body: String, val checked: Boolean, val onChange: (Boolean) -> Unit)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val retainTranscript by viewModel.retainTranscript.collectAsState()
    val lowBatteryMode by viewModel.lowBatteryMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    var deleteFailed by remember { mutableStateOf(false) }

    val items = buildList {
        add(
            SettingItem(
                stringResource(R.string.settings_low_battery_mode),
                stringResource(R.string.settings_low_battery_mode_body),
                lowBatteryMode,
                viewModel::setLowBatteryMode,
            ),
        )
        add(
            SettingItem(
                stringResource(R.string.settings_retain_transcript),
                stringResource(R.string.settings_retain_transcript_body),
                retainTranscript,
                viewModel::setRetainTranscript,
            ),
        )
        add(
            SettingItem(
                stringResource(R.string.settings_notifications),
                stringResource(R.string.settings_notifications_body),
                notificationsEnabled,
                viewModel::setNotificationsEnabled,
            ),
        )
        // Wallpaper-derived color only exists on Android 12+; hide the toggle where it does nothing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                SettingItem(
                    stringResource(R.string.settings_dynamic_color),
                    stringResource(R.string.settings_dynamic_color_body),
                    dynamicColor,
                    viewModel::setDynamicColor,
                ),
            )
        }
    }

    SettingsContent(
        items = items,
        deleteFailed = deleteFailed,
        onDeleteAccount = {
            deleteFailed = false
            viewModel.deleteAccount { success -> deleteFailed = !success }
        },
    )
}

@Composable
private fun SettingsContent(items: List<SettingItem>, deleteFailed: Boolean, onDeleteAccount: () -> Unit) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        ScreenHeader(overline = stringResource(R.string.app_name), title = stringResource(R.string.settings_title))
        NudgeCard {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                SettingRow(item)
            }
        }
        // `error` is a neutral grey in this theme by design: destructive, but never alarming.
        TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.padding(top = Spacing.lg)) {
            Text(stringResource(R.string.settings_delete_account), color = MaterialTheme.colorScheme.error)
        }
        if (deleteFailed) {
            Text(
                stringResource(R.string.settings_delete_account_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = { Text(stringResource(R.string.settings_delete_account_body)) },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDeleteAccount() }) {
                    Text(stringResource(R.string.settings_delete_account_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingRow(item: SettingItem) {
    // The whole row is one toggle, so TalkBack reads title, description and state together and
    // the touch target is the full row. The Switch itself is display-only (onCheckedChange = null).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizing.minTouchTarget)
            .toggleable(value = item.checked, role = Role.Switch, onValueChange = item.onChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = item.checked, onCheckedChange = null)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1EFE8, heightDp = 640)
@Composable
private fun SettingsPreview() {
    NudgeTheme {
        SettingsContent(
            items = listOf(
                SettingItem("Low battery mode", "Only show easy tasks in the widget.", true) {},
                SettingItem("Keep transcripts", "Off deletes each memo's text after tasks are extracted.", true) {},
                SettingItem("Daily reminder", "At most one a day. Off by default.", false) {},
            ),
            deleteFailed = false,
            onDeleteAccount = {},
        )
    }
}
