package com.zerotap.app.ui.mission

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotap.app.core.ActionRecord
import com.zerotap.app.core.MissionState
import com.zerotap.app.core.RunStatus
import com.zerotap.app.core.StepStatus
import com.zerotap.app.core.TimelineStep
import com.zerotap.app.ui.Tab
import com.zerotap.app.ui.ZeroTapActions
import com.zerotap.app.ui.components.GhostButton
import com.zerotap.app.ui.components.GlassChip
import com.zerotap.app.ui.components.GlassSurface
import com.zerotap.app.ui.components.GlowButton
import com.zerotap.app.ui.components.GlowProgress
import com.zerotap.app.ui.components.SectionLabel
import com.zerotap.app.ui.components.StatusDot
import com.zerotap.app.ui.components.clickableChip
import com.zerotap.app.ui.components.runStatusColor
import com.zerotap.app.ui.components.runStatusLabel
import com.zerotap.app.ui.theme.ZtGreen
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtRose
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType

@Composable
fun MissionScreen(
    mission: MissionState,
    onStop: () -> Unit,
    actions: ZeroTapActions
) {
    if (mission.status == RunStatus.IDLE) {
        MissionEmpty(onStart = { actions.goToTab(Tab.HOME) })
        return
    }

    val accent = runStatusColor(mission.status)
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item { MissionHeader(mission, accent, onStop) }
        item { MissionStats(mission) }
        if (mission.blocker != null) item { BlockerCard(mission.blocker!!) }
        item { TimelineCard(mission.timeline, accent) }
        if (mission.memoryRefs.isNotEmpty()) item { MemoryRefs(mission.memoryRefs) }
        item { ActionLogCard(mission.actionLog) }
    }
}

@Composable
private fun MissionHeader(mission: MissionState, accent: Color, onStop: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), glow = accent.copy(alpha = 0.45f)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(accent, active = mission.isActive)
                Spacer(Modifier.width(10.dp))
                SectionLabel(runStatusLabel(mission.status).uppercase(), color = accent)
                Spacer(Modifier.weight(1f))
                Text("${(mission.progress * 100).toInt()}%", style = ZtType.body, color = ZtInkDim)
            }
            Spacer(Modifier.height(14.dp))
            Text(mission.goal, style = ZtType.title, color = ZtInk)
            Spacer(Modifier.height(8.dp))
            Text(
                mission.currentAction.ifBlank { mission.understanding },
                style = ZtType.body,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            GlowProgress(mission.progress, Modifier.fillMaxWidth().height(7.dp), accent)
            if (mission.isActive) {
                Spacer(Modifier.height(16.dp))
                GlowButton(
                    text = "Stop mission",
                    onClick = onStop,
                    icon = Icons.Rounded.Stop,
                    colors = listOf(ZtRose, Color(0xFFFF9A6B)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MissionStats(mission: MissionState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("App", mission.currentApp.ifBlank { "—" }, Modifier.weight(1f))
        StatTile("Screen", mission.currentScreen.ifBlank { "—" }, Modifier.weight(1f))
        StatTile("Confidence", "${(mission.confidence * 100).toInt()}%", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier) {
        Column(Modifier.padding(14.dp)) {
            SectionLabel(label)
            Spacer(Modifier.height(8.dp))
            Text(value, style = ZtType.body, color = ZtInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BlockerCard(blocker: String) {
    GlassSurface(Modifier.fillMaxWidth(), glow = ZtRose.copy(alpha = 0.4f)) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("BLOCKED", color = ZtRose)
            Spacer(Modifier.height(8.dp))
            Text(blocker, style = ZtType.body, color = ZtInk)
        }
    }
}

@Composable
private fun TimelineCard(timeline: List<TimelineStep>, accent: Color) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("MISSION TIMELINE")
            Spacer(Modifier.height(14.dp))
            if (timeline.isEmpty()) {
                Text("Building the plan…", style = ZtType.small, color = ZtInkFaint)
            } else {
                timeline.forEach { step -> TimelineRow(step, accent) }
            }
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (step.status) {
            StepStatus.DONE -> Icon(Icons.Rounded.Check, null, tint = ZtGreen, modifier = Modifier.size(20.dp))
            StepStatus.ACTIVE -> PulsingDot(accent, active = true)
            StepStatus.FAILED -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = ZtRose, modifier = Modifier.size(20.dp))
            else -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = ZtInkFaint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            step.label,
            style = ZtType.body,
            color = when (step.status) {
                StepStatus.DONE -> ZtInkDim
                StepStatus.ACTIVE -> ZtInk
                else -> ZtInkFaint
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MemoryRefs(refs: List<String>) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("USING YOUR PREFERENCES")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                refs.forEach { ref ->
                    GlassChip(text = ref, accent = ZtTeal, leadingDot = true)
                }
            }
        }
    }
}

@Composable
private fun ActionLogCard(log: List<ActionRecord>) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("ACTION LOG")
            Spacer(Modifier.height(12.dp))
            if (log.isEmpty()) {
                Text("No actions yet.", style = ZtType.small, color = ZtInkFaint)
            } else {
                log.takeLast(12).reversed().forEach { record ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(if (record.success) ZtGreen else ZtRose, 7)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            record.detail,
                            style = ZtType.mono,
                            color = ZtInkDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionEmpty(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = ZtTeal, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("No active mission", style = ZtType.title, color = ZtInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Give ZeroTap a task and watch it work, step by step.",
            style = ZtType.body,
            color = ZtInkFaint
        )
        Spacer(Modifier.height(24.dp))
        GhostButton(text = "Go to command center", onClick = onStart, accent = ZtTeal)
    }
}

@Composable
private fun PulsingDot(color: Color, active: Boolean) {
    if (!active) {
        StatusDot(color)
        return
    }
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(Modifier.alpha(alpha)) { StatusDot(color) }
}
