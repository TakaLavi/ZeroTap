package com.zerotap.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotap.app.api.ApiConfig
import com.zerotap.app.ui.ZeroTapActions
import com.zerotap.app.ui.components.GhostButton
import com.zerotap.app.ui.components.GlassSurface
import com.zerotap.app.ui.components.GlowButton
import com.zerotap.app.ui.components.SectionLabel
import com.zerotap.app.ui.components.clickableChip
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType

private val PLANNING_MODELS = listOf("mimo-v2.5-pro", "mimo-v2.5", "mimo-v2-pro")
private val VISION_MODELS = listOf("mimo-v2-omni", "mimo-v2.5-pro")

@Composable
fun SettingsScreen(
    config: ApiConfig,
    onSaveApiKey: (String) -> Unit,
    onSaveBaseUrl: (String) -> Unit,
    onSavePlanningModel: (String) -> Unit,
    onSaveVisionModel: (String) -> Unit,
    actions: ZeroTapActions
) {
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var planning by remember(config.planningModel) { mutableStateOf(config.planningModel) }
    var vision by remember(config.visionModel) { mutableStateOf(config.visionModel) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSurface(Modifier.fillMaxWidth(), glow = ZtTeal.copy(alpha = 0.35f)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Settings", style = ZtType.title, color = ZtInk)
                    Spacer(Modifier.height(4.dp))
                    Text("Configure the MiMo reasoning engine.", style = ZtType.small, color = ZtInkFaint)
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    SectionLabel("MIMO API KEY")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldBox(Modifier.weight(1f)) {
                            BasicTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it; saved = false },
                                textStyle = ZtType.mono.copy(color = ZtInk),
                                cursorBrush = SolidColor(ZtTeal),
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                decorationBox = { inner ->
                                    if (apiKey.isEmpty()) Text("tp-…", style = ZtType.mono, color = ZtInkFaint)
                                    inner()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier.clickableChip { showKey = !showKey }.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null, tint = ZtInkDim, modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Stored only on this device. Never committed to source.",
                        style = ZtType.small, color = ZtInkFaint
                    )
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    SectionLabel("ENDPOINT")
                    Spacer(Modifier.height(12.dp))
                    FieldBox(Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it; saved = false },
                            textStyle = ZtType.mono.copy(color = ZtInk),
                            cursorBrush = SolidColor(ZtTeal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    SectionLabel("PLANNING MODEL")
                    Spacer(Modifier.height(12.dp))
                    ModelChips(PLANNING_MODELS, planning) { planning = it; saved = false }
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("VISION MODEL")
                    Spacer(Modifier.height(12.dp))
                    ModelChips(VISION_MODELS, vision) { vision = it; saved = false }
                }
            }
        }

        item {
            GlowButton(
                text = if (saved) "Saved" else "Save settings",
                onClick = {
                    onSaveApiKey(apiKey)
                    onSaveBaseUrl(baseUrl)
                    onSavePlanningModel(planning)
                    onSaveVisionModel(vision)
                    saved = true
                },
                icon = if (saved) Icons.Rounded.Check else null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    SectionLabel("PERMISSIONS")
                    Spacer(Modifier.height(12.dp))
                    GhostButton("Open accessibility settings", actions.onOpenAccessibility, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Grant screen vision", actions.onRequestProjection, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Notification permission", actions.onRequestNotifications, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    GhostButton("App info", actions.onOpenAppSettings, Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Text(
                "ZeroTap 1.0 • An autonomous phone agent powered by Xiaomi MiMo.",
                style = ZtType.small,
                color = ZtInkFaint,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
private fun ModelChips(models: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        models.forEach { model ->
            val active = model == selected
            Box(
                Modifier
                    .clickableChip { onSelect(model) }
                    .background(
                        if (active) ZtTeal.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (active) ZtTeal.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f),
                        CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    model,
                    style = ZtType.small,
                    color = if (active) ZtTeal else ZtInkDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
