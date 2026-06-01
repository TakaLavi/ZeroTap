package com.zerotap.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Clickable without the default ripple, used for custom glass controls. */
fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick
)

/** Convenience rippleless click that manages its own interaction source. */
fun Modifier.clickableChip(onClick: () -> Unit): Modifier = composed {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
