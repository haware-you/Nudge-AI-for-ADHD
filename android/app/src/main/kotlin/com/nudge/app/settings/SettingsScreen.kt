package com.nudge.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudge.app.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val retainTranscript by viewModel.retainTranscript.collectAsState()
    val lowBatteryMode by viewModel.lowBatteryMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
