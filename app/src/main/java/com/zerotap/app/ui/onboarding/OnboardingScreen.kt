package com.zerotap.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zerotap.app.ui.components.GhostButton
import com.zerotap.app.ui.components.GlassSurface
import com.zerotap.app.ui.components.GlowButton
import com.zerotap.app.ui.components.SectionLabel
import com.zerotap.app.ui.components.StatusDot
import com.zerotap.app.ui.theme.ZtBlue
import com.zerotap.app.ui.theme.ZtGreen
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType
import com.zerotap.app.ui.theme.ZtViolet

@Composable
fun OnboardingScreen(
    accessibilityOn: Boolean,
    captureOn: Boolean,
    apiReady: Boolean,
    onOpenAccessibility: () -> Unit,
    onRequestProjection: () -> Unit,
    onConfigureApi: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier
                .size(92.dp)
                .background(
                    Brush.linearGradient(listOf(ZtTeal.copy(alpha = 0.25f), ZtBlue.copy(alpha = 0.15f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = ZtTeal, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("ZeroTap", style = ZtType.hero, color = ZtInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your autonomous phone operator. One sentence — and it does the tapping for you.",
            style = ZtType.body,
            color = ZtInkDim
        )
        Spacer(Modifier.height(32.dp))

        PermissionStep(
            icon = Icons.Rounded.Accessibility,
            title = "Accessibility control",
            body = "ZeroTap reads the current screen and taps, types and scrolls for you. You stay in control and can stop anytime.",
            done = accessibilityOn,
            cta = "Enable",
            accent = ZtTeal,
            onClick = onOpenAccessibility
        )
        Spacer(Modifier.height(12.dp))
        PermissionStep(
            icon = Icons.Rounded.Visibility,
            title = "Screen vision",
            body = "When the accessibility data isn't enough, ZeroTap looks at a screenshot to understand what's on screen.",
            done = captureOn,
            cta = "Grant",
            accent = ZtBlue,
            onClick = onRequestProjection
        )
        Spacer(Modifier.height(12.dp))
        PermissionStep(
            icon = Icons.Rounded.Key,
            title = "MiMo API key",
            body = "ZeroTap thinks with the Xiaomi MiMo models. Add your key to power planning and reasoning.",
            done = apiReady,
            cta = "Add key",
            accent = ZtViolet,
            onClick = onConfigureApi
        )

        Spacer(Modifier.height(20.dp))
        GlassSurface(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = ZtGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "ZeroTap always asks before anything irreversible — sending, buying, posting or deleting.",
                    style = ZtType.small,
                    color = ZtInkDim
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        GlowButton(
            text = "Enter ZeroTap",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GhostButton(text = "Skip for now", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionStep(
    icon: ImageVector,
    title: String,
    body: String,
    done: Boolean,
    cta: String,
    accent: Color,
    onClick: () -> Unit
) {
    GlassSurface(Modifier.fillMaxWidth(), glow = if (done) accent.copy(alpha = 0.3f) else null) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(title, style = ZtType.section, color = ZtInk, modifier = Modifier.weight(1f))
                if (done) {
                    StatusDot(ZtGreen)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(body, style = ZtType.small, color = ZtInkFaint)
            Spacer(Modifier.height(14.dp))
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = ZtGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ready", style = ZtType.small, color = ZtGreen)
                }
            } else {
                GhostButton(text = cta, onClick = onClick, accent = accent)
            }
        }
    }
}
