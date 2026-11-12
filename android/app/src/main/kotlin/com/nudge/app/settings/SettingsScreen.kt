package com.nudge.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import com.nudge.app.R
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val retainTranscript by viewModel.retainTranscript.collectAsState()
    val lowBatteryMode by viewModel.lowBatteryMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        SettingRow(
            title = stringResource(R.string.settings_retain_transcript),
            body = stringResource(R.string.settings_retain_transcript_body),
            checked = retainTranscript,
            onCheckedChange = viewModel::setRetainTranscript,
        )
        SettingRow(
            title = stringResource(R.string.settings_low_battery_mode),
            body = stringResource(R.string.settings_low_battery_mode_body),
            checked = lowBatteryMode,
            onCheckedChange = viewModel::setLowBatteryMode,
        )
        // Wallpaper-derived color only exists on Android 12+; hide the toggle where it does nothing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingRow(
                title = stringResource(R.string.settings_dynamic_color),
                body = stringResource(R.string.settings_dynamic_color_body),
                checked = dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
        }
        SettingRow(
            title = stringResource(R.string.settings_notifications),
            body = stringResource(R.string.settings_notifications_body),
            checked = notificationsEnabled,
            onCheckedChange = viewModel::setNotificationsEnabled,
        )
    }
}

@Composable
private fun SettingRow(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The whole row is one toggle, so TalkBack reads title, description and state together and
    // the touch target is the full row. The Switch itself is display-only (onCheckedChange = null).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizing.minTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
