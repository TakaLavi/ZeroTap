package com.zerotap.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.zerotap.app.ui.theme.ZtBg
import com.zerotap.app.ui.theme.ZtBgDeep
import com.zerotap.app.ui.theme.ZtBlue
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtViolet

/** Slowly drifting aurora blobs over near-black — the living backdrop of the whole app. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t2"
    )

    Canvas(modifier.fillMaxSize().background(ZtBgDeep)) {
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(ZtBg, ZtBgDeep)))

        drawBlob(ZtTeal.copy(alpha = 0.34f), Offset(w * (0.18f + 0.12f * t1), h * (0.14f + 0.04f * t2)), w * 0.62f)
        drawBlob(ZtBlue.copy(alpha = 0.30f), Offset(w * (0.88f - 0.12f * t2), h * (0.30f + 0.06f * t1)), w * 0.70f)
        drawBlob(ZtViolet.copy(alpha = 0.26f), Offset(w * (0.45f + 0.06f * t2), h * (0.86f - 0.05f * t1)), w * 0.82f)

        // top darkening so status bar text stays readable
        drawRect(
            Brush.verticalGradient(
                0f to ZtBgDeep.copy(alpha = 0.65f),
                0.18f to Color.Transparent,
                startY = 0f, endY = h * 0.35f
            )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(
    color: Color,
    center: Offset,
    radius: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
