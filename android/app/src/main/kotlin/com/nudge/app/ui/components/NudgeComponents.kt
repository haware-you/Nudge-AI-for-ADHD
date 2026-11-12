package com.nudge.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

/**
 * Shared building blocks for the in-app screens. The visual language: warm paper background,
 * lighter bordered cards with no shadow, uppercase tracked overlines, round action buttons,
 * and a floating pill bottom bar.
 */

/** Short uppercase label for sections and meta lines. Never use for sentences or task text. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Screen title block: overline, large title, and an optional trailing action. */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = Spacing.lg),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Overline(overline)
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        if (action != null) action()
    }
}

/** Flat bordered card. Elevation is not used anywhere in the app; borders separate surfaces. */
@Composable
fun NudgeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Sizing.border, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}

/**
 * Round icon action, 48dp minimum. Filled = the primary thing to do here; outlined = a
 * secondary or not-yet-done action.
 */
@Composable
fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    size: Dp = Sizing.minTouchTarget,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (filled) colors.primary else Color.Transparent,
        contentColor = if (filled) colors.onPrimary else colors.onSurfaceVariant,
        border = if (filled) null else BorderStroke(Sizing.borderStrong, colors.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

data class BottomBarItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Floating pill navigation. The selected item gets a tinted rounded tile behind it. */
@Composable
fun NudgeBottomBar(items: List<BottomBarItem>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(Sizing.border, colors.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items.forEach { item ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(Sizing.bottomBarItemHeight)
                        .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick),
                    shape = MaterialTheme.shapes.medium,
                    color = if (item.selected) colors.secondaryContainer else Color.Transparent,
                    contentColor = if (item.selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(item.icon, contentDescription = null)
                        Text(item.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1EFE8)
@Composable
private fun ComponentsPreview() {
    NudgeTheme {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ScreenHeader(overline = "Nudge", title = "Review") {
                CircleActionButton(Icons.Filled.Mic, "Capture", onClick = {})
            }
            NudgeCard {
                Text("Email the landlord about the tap", style = MaterialTheme.typography.titleMedium)
                Overline("From this memo", modifier = Modifier.padding(top = Spacing.xs))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                CircleActionButton(Icons.Filled.Check, "Done", onClick = {})
                CircleActionButton(Icons.Filled.Check, "Confirm", onClick = {}, filled = false)
            }
            NudgeBottomBar(
                listOf(
                    BottomBarItem("Review", Icons.AutoMirrored.Filled.List, selected = true, onClick = {}),
                    BottomBarItem("Settings", Icons.Filled.Settings, selected = false, onClick = {}),
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF171A16)
@Composable
private fun ComponentsDarkPreview() {
    NudgeTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            NudgeCard {
                Text(
                    "Email the landlord about the tap",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Overline("From this memo", modifier = Modifier.padding(top = Spacing.xs))
            }
            CircleActionButton(Icons.Filled.Check, "Done", onClick = {})
        }
    }
}
