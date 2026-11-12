package com.nudge.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.nudge.app.ui.theme.NudgeMotion
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

/**
 * Shared building blocks for the in-app screens. The visual language: a quiet grey page, plain
 * borderless cards, one tinted primary action per screen, and floating translucent chrome that
 * content scrolls under. Every tappable thing answers on press-down ([pressScale]).
 */

/** Instant, physical press feedback: shrinks slightly while a finger is down. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) NudgeMotion.PRESSED_SCALE else 1f,
        animationSpec = NudgeMotion.press(),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * One haptic tick for a committed action (stop recording, confirm a task). Used sparingly on
 * purpose: feedback that fires everywhere gets ignored.
 */
@Composable
fun rememberConfirmHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.VIRTUAL_KEY,
            )
        }
    }
}

/** Short secondary tag for meta lines. Never use for sentences or task text. */
@Composable
fun MetaLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Screen title block: one large title and an optional trailing action. Nothing else. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        if (action != null) action()
    }
}

/** Plain card: lighter than the page, no border, no shadow. Grouping comes from the fill alone. */
@Composable
fun NudgeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.size(size).pressScale(interactionSource),
        shape = CircleShape,
        color = if (filled) colors.primary else Color.Transparent,
        contentColor = if (filled) colors.onPrimary else colors.onSurfaceVariant,
        border = if (filled) null else BorderStroke(Sizing.borderStrong, colors.outline),
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

data class BottomBarItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/**
 * Floating capsule navigation. It is slightly translucent and sits over the content, so lists
 * scroll under it; screens leave [Sizing.bottomBarClearance] at the bottom. Real backdrop blur
 * is not used: Compose has no built-in one below Android 12.
 */
@Composable
fun NudgeBottomBar(items: List<BottomBarItem>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.md)
            .fillMaxWidth(),
        shape = CircleShape,
        color = colors.surfaceContainerLowest.copy(alpha = NudgeMotion.CHROME_ALPHA),
        border = BorderStroke(Sizing.hairline, colors.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items.forEach { item ->
                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(Sizing.bottomBarItemHeight)
                        .pressScale(interactionSource)
                        .selectable(
                            selected = item.selected,
                            interactionSource = interactionSource,
                            indication = null, // the press scale is the feedback
                            role = Role.Tab,
                            onClick = item.onClick,
                        ),
                    shape = CircleShape,
                    color = if (item.selected) colors.secondaryContainer else Color.Transparent,
                    contentColor = if (item.selected) colors.primary else colors.onSurfaceVariant,
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

@Preview(showBackground = true, backgroundColor = 0xFFF2F3F1)
@Composable
private fun ComponentsPreview() {
    NudgeTheme {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ScreenHeader(title = "Review") {
                CircleActionButton(Icons.Filled.Mic, "Capture", onClick = {})
            }
            NudgeCard {
                Text("Email the landlord about the tap", style = MaterialTheme.typography.titleMedium)
                MetaLabel("From this memo", modifier = Modifier.padding(top = Spacing.xs))
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
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
                MetaLabel("From this memo", modifier = Modifier.padding(top = Spacing.xs))
            }
            CircleActionButton(Icons.Filled.Check, "Done", onClick = {})
        }
    }
}
