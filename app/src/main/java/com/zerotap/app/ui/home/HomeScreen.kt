package com.zerotap.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotap.app.api.ApiConfig
import com.zerotap.app.core.MemoryItem
import com.zerotap.app.core.RunStatus
import com.zerotap.app.data.db.TaskRecordEntity
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
import com.zerotap.app.ui.components.relativeTime
import com.zerotap.app.ui.components.runStatusColor
import com.zerotap.app.ui.components.runStatusLabel
import com.zerotap.app.ui.theme.ZtAmber
import com.zerotap.app.ui.theme.ZtBlue
import com.zerotap.app.ui.theme.ZtGreen
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtRose
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType
import com.zerotap.app.ui.theme.ZtViolet
import kotlinx.coroutines.delay

private val SUGGESTIONS = listOf(
    "Find me dinner nearby",
    "Message Bethany on Discord",
    "Find the cheapest gaming mouse",
    "Compare laptops under \$500",
    "Find sneakers under \$20",
    "Research this product"
)

@Composable
fun HomeScreen(
    config: ApiConfig,
    accessibilityOn: Boolean,
    captureOn: Boolean,
    missionStatus: RunStatus,
    missionGoal: String,
    missionProgress: Float,
    missionAction: String,
    memories: List<MemoryItem>,
    history: List<TaskRecordEntity>,
    onRun: (String) -> Unit,
    onStop: () -> Unit,
    actions: ZeroTapActions
) {
    var command by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            HeroConsole(
                command = command,
                onCommand = { command = it },
                ready = config.isReady,
                onRun = {
                    if (command.isNotBlank()) {
                        onRun(command)
                        command = ""
                    }
                }
            )
        }
        item { SuggestionRow(onPick = { command = it }) }
        item {
            PermissionPanel(
                accessibilityOn = accessibilityOn,
                captureOn = captureOn,
                apiReady = config.isReady,
                actions = actions
            )
        }
        if (missionStatus != RunStatus.IDLE) {
            item {
                ActivePeek(
                    status = missionStatus,
                    goal = missionGoal,
                    progress = missionProgress,
                    action = missionAction,
                    onOpen = { actions.goToTab(Tab.MISSION) },
                    onStop = onStop
                )
            }
        }
        item { MemoryStrip(memories = memories, onOpen = { actions.goToTab(Tab.MEMORY) }) }
        item { RecentTasksPanel(history = history, onPick = { command = it }) }
        item { ModelPanel(config = config) }
    }
}

@Composable
private fun HeroConsole(
    command: String,
    onCommand: (String) -> Unit,
    ready: Boolean,
    onRun: () -> Unit
) {
    var placeholderIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            placeholderIndex = (placeholderIndex + 1) % SUGGESTIONS.size
        }
    }

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glow = ZtTeal.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("COMMAND CENTER")
                Spacer(Modifier.width(10.dp))
                StatusDot(if (ready) ZtGreen else ZtAmber, 7)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.heightIn(min = 64.dp)) {
                if (command.isEmpty()) {
                    AnimatedContent(
                        targetState = placeholderIndex,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "placeholder"
                    ) { index ->
                        Text(
                            "“${SUGGESTIONS[index]}”",
                            style = ZtType.hero,
                            color = ZtInkFaint
                        )
                    }
                }
                BasicTextField(
                    value = command,
                    onValueChange = onCommand,
                    textStyle = ZtType.hero.copy(color = ZtInk),
                    cursorBrush = SolidColor(ZtTeal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowButton(
                    text = "Run task",
                    onClick = onRun,
                    enabled = command.isNotBlank(),
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
                GhostButton(
                    text = "Clear",
                    onClick = { onCommand("") }
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SUGGESTIONS.take(5).forEach { suggestion ->
            Box(Modifier.clickableChip { onPick(suggestion) }) {
                GlassChip(text = suggestion, accent = ZtBlue)
            }
        }
    }
}

@Composable
private fun PermissionPanel(
    accessibilityOn: Boolean,
    captureOn: Boolean,
    apiReady: Boolean,
    actions: ZeroTapActions
) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("PERMISSIONS")
            Spacer(Modifier.height(14.dp))
            PermissionRow(
                title = "Accessibility control",
                subtitle = "Lets ZeroTap tap, type and navigate apps",
                ok = accessibilityOn,
                onFix = actions.onOpenAccessibility
            )
            Spacer(Modifier.height(10.dp))
            PermissionRow(
                title = "Screen vision",
                subtitle = "Lets ZeroTap see the screen when needed",
                ok = captureOn,
                onFix = actions.onRequestProjection
            )
            Spacer(Modifier.height(10.dp))
            PermissionRow(
                title = "MiMo API key",
                subtitle = "Powers planning and reasoning",
                ok = apiReady,
                onFix = { actions.goToTab(Tab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, ok: Boolean, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(if (ok) ZtGreen else ZtAmber)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = ZtType.body, color = ZtInk)
            Text(subtitle, style = ZtType.small, color = ZtInkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (ok) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = ZtGreen, modifier = Modifier.size(20.dp))
        } else {
            GhostButton(text = "Enable", onClick = onFix, accent = ZtAmber)
        }
    }
}

@Composable
private fun ActivePeek(
    status: RunStatus,
    goal: String,
    progress: Float,
    action: String,
    onOpen: () -> Unit,
    onStop: () -> Unit
) {
    val accent = runStatusColor(status)
    GlassSurface(Modifier.fillMaxWidth().clickableChip(onOpen), glow = accent.copy(alpha = 0.4f)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(accent)
                Spacer(Modifier.width(10.dp))
                SectionLabel(runStatusLabel(status).uppercase(), color = accent)
                Spacer(Modifier.weight(1f))
                if (status == RunStatus.RUNNING || status == RunStatus.PLANNING || status == RunStatus.WAITING_CONFIRMATION) {
                    Box(Modifier.clickableChip(onStop)) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = ZtRose, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(goal, style = ZtType.title, color = ZtInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(action, style = ZtType.small, color = ZtInkDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            GlowProgress(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp), color = accent)
        }
    }
}

@Composable
private fun MemoryStrip(memories: List<MemoryItem>, onOpen: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Memory, contentDescription = null, tint = ZtViolet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("WHAT I REMEMBER")
                Spacer(Modifier.weight(1f))
                Text("${memories.size}", style = ZtType.small, color = ZtInkFaint)
            }
            Spacer(Modifier.height(14.dp))
            if (memories.isEmpty()) {
                Text("Nothing yet — I'll learn your preferences as we go.", style = ZtType.small, color = ZtInkFaint)
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    memories.take(8).forEach { item ->
                        MemoryMiniCard(item)
                    }
                }
                Spacer(Modifier.height(12.dp))
                GhostButton(text = "Open Memory Center", onClick = onOpen, accent = ZtViolet)
            }
        }
    }
}

@Composable
private fun MemoryMiniCard(item: MemoryItem) {
    Box(
        Modifier
            .width(160.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            SectionLabel(item.kind.name, color = ZtTeal)
            Spacer(Modifier.height(6.dp))
            Text(item.key, style = ZtType.body, color = ZtInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.value, style = ZtType.small, color = ZtInkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentTasksPanel(history: List<TaskRecordEntity>, onPick: (String) -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = ZtBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("RECENT TASKS")
            }
            Spacer(Modifier.height(12.dp))
            if (history.isEmpty()) {
                Text("Your completed tasks will appear here.", style = ZtType.small, color = ZtInkFaint)
            } else {
                history.take(6).forEach { task ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickableChip { onPick(task.goal) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(if (task.status == "COMPLETED") ZtGreen else ZtInkFaint, 7)
                        Spacer(Modifier.width(12.dp))
                        Text(task.goal, style = ZtType.body, color = ZtInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(relativeTime(task.createdAt), style = ZtType.small, color = ZtInkFaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelPanel(config: ApiConfig) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ZtTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("REASONING ENGINE")
                Spacer(Modifier.weight(1f))
                StatusDot(if (config.isReady) ZtGreen else ZtRose, 7)
            }
            Spacer(Modifier.height(12.dp))
            ModelRow("Planning", config.planningModel)
            Spacer(Modifier.height(8.dp))
            ModelRow("Vision", config.visionModel)
            Spacer(Modifier.height(8.dp))
            ModelRow("Endpoint", config.baseUrl.removePrefix("https://").substringBefore("/"))
        }
    }
}

@Composable
private fun ModelRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ZtType.small, color = ZtInkFaint, modifier = Modifier.width(84.dp))
        Text(value.ifBlank { "—" }, style = ZtType.mono, color = ZtInkDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
