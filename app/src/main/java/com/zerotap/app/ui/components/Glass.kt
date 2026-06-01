package com.zerotap.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotap.app.ui.theme.GlassHi
import com.zerotap.app.ui.theme.GlassLo
import com.zerotap.app.ui.theme.GlassScrim
import com.zerotap.app.ui.theme.GlassStroke
import com.zerotap.app.ui.theme.GlassStrokeSoft
import com.zerotap.app.ui.theme.ZtBlue
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtShapes
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType

/** A floating translucent glass surface with depth, used for every card and panel. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ZtShapes.large,
    glow: Color? = null,
    strokeBrush: Brush = Brush.linearGradient(listOf(GlassStroke, GlassStrokeSoft)),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .shadow(
                elevation = if (glow != null) 30.dp else 20.dp,
                shape = shape,
                clip = false,
                ambientColor = (glow ?: Color.Black).copy(alpha = 0.35f),
                spotColor = (glow ?: Color.Black).copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(GlassScrim)
            .background(Brush.verticalGradient(listOf(GlassHi, GlassLo)))
            .border(BorderStroke(1.dp, strokeBrush), shape),
        content = content
    )
}

/** Tiny pill chip on glass. */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = ZtTeal,
    leadingDot: Boolean = false
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leadingDot) StatusDot(color = accent, size = 7)
            Text(text, style = ZtType.label, color = ZtInkDim)
        }
    }
}

/** A glowing status dot. */
@Composable
fun StatusDot(color: Color, size: Int = 9) {
    Box(
        Modifier
            .size(size.dp)
            .drawBehind {
                drawCircle(color = color.copy(alpha = 0.30f), radius = this.size.minDimension)
            }
            .clip(CircleShape)
            .background(color)
    )
}

/** Small uppercase tracking label for section headers. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = ZtInkFaint) {
    Text(text.uppercase(), style = ZtType.label, color = color, modifier = modifier)
}

/** Primary pill button with a soft accent glow and press animation. */
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: List<Color> = listOf(ZtTeal, ZtBlue)
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val fill = if (enabled) colors else listOf(Color(0xFF273043), Color(0xFF1B2233))

    Box(
        modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 22.dp else 0.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = colors.first().copy(alpha = 0.5f),
                spotColor = colors.first().copy(alpha = 0.6f)
            )
            .clip(CircleShape)
            .background(Brush.horizontalGradient(fill))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickableNoRipple(enabled, interaction, onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFF021A16), modifier = Modifier.size(19.dp))
            }
            Text(
                text,
                style = ZtType.section,
                color = if (enabled) Color(0xFF021A16) else ZtInkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Subtle outlined glass button. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = ZtInk
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    Box(
        modifier
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickableNoRipple(true, interaction, onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(text, style = ZtType.body, color = accent, maxLines = 1)
        }
    }
}

/** A thin glowing progress track. The caller sets the height via [modifier]. */
@Composable
fun GlowProgress(progress: Float, modifier: Modifier = Modifier, color: Color = ZtTeal) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color, ZtBlue)))
        )
    }
}
