package com.zerotap.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotap.app.AppContainer
import com.zerotap.app.core.ConfirmationRequest
import com.zerotap.app.core.MissionState
import com.zerotap.app.core.RunStatus
import com.zerotap.app.ui.components.AuroraBackground
import com.zerotap.app.ui.components.GhostButton
import com.zerotap.app.ui.components.GlassSurface
import com.zerotap.app.ui.components.GlowButton
import com.zerotap.app.ui.components.SectionLabel
import com.zerotap.app.ui.components.StatusDot
import com.zerotap.app.ui.components.clickableChip
import com.zerotap.app.ui.components.runStatusColor
import com.zerotap.app.ui.components.runStatusLabel
import com.zerotap.app.ui.home.HomeScreen
import com.zerotap.app.ui.memory.MemoryScreen
import com.zerotap.app.ui.mission.MissionScreen
import com.zerotap.app.ui.onboarding.OnboardingScreen
import com.zerotap.app.ui.settings.SettingsScreen
import com.zerotap.app.ui.theme.ZtAmber
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType

@Composable
fun ZeroTapRoot(
    container: AppContainer,
    onRequestProjection: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    CompositionLocalProvider(LocalContainer provides container) {
        val vm: ZeroTapViewModel = viewModel()

        val mission by vm.mission.collectAsStateWithLifecycle()
        val config by vm.config.collectAsStateWithLifecycle()
        val onboarded by vm.onboarded.collectAsStateWithLifecycle()
        val accessibilityOn by vm.accessibilityConnected.collectAsStateWithLifecycle()
        val captureOn by vm.captureActive.collectAsStateWithLifecycle()
        val memories by vm.memories.collectAsStateWithLifecycle()
        val history by vm.history.collectAsStateWithLifecycle()

        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

        val actions = remember {
            ZeroTapActions(
                onRequestProjection = onRequestProjection,
                onRequestNotifications = onRequestNotifications,
                onOpenAccessibility = onOpenAccessibility,
                onOpenAppSettings = onOpenAppSettings,
                goToTab = { tab = it }
            )
        }

        LaunchedEffect(mission.isActive) {
            if (mission.isActive) tab = Tab.MISSION
        }

        Box(Modifier.fillMaxSize()) {
            AuroraBackground()

            if (!onboarded) {
                Box(Modifier.statusBarsPadding()) {
                    OnboardingScreen(
                        accessibilityOn = accessibilityOn,
                        captureOn = captureOn,
                        apiReady = config.isReady,
                        onOpenAccessibility = onOpenAccessibility,
                        onRequestProjection = onRequestProjection,
                        onConfigureApi = { vm.setOnboarded(true); tab = Tab.SETTINGS },
                        onFinish = { vm.setOnboarded(true) }
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    TopHeader(mission = mission, modelName = config.planningModel)
                    Box(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab"
                        ) { current ->
                            when (current) {
                                Tab.HOME -> HomeScreen(
                                    config = config,
                                    accessibilityOn = accessibilityOn,
                                    captureOn = captureOn,
                                    missionStatus = mission.status,
                                    missionGoal = mission.goal,
                                    missionProgress = mission.progress,
                                    missionAction = mission.currentAction,
                                    memories = memories,
                                    history = history,
                                    onRun = { vm.run(it) },
                                    onStop = { vm.stop() },
                                    actions = actions
                                )
                                Tab.MISSION -> MissionScreen(
                                    mission = mission,
                                    onStop = { vm.stop() },
                                    actions = actions
                                )
                                Tab.MEMORY -> MemoryScreen(
                                    memories = memories,
                                    onPin = { vm.pinMemory(it) },
                                    onLock = { vm.lockMemory(it) },
                                    onDelete = { vm.deleteMemory(it) }
                                )
                                Tab.SETTINGS -> SettingsScreen(
                                    config = config,
                                    onSaveApiKey = { vm.saveApiKey(it) },
                                    onSaveBaseUrl = { vm.saveBaseUrl(it) },
                                    onSavePlanningModel = { vm.savePlanningModel(it) },
                                    onSaveVisionModel = { vm.saveVisionModel(it) },
                                    actions = actions
                                )
                            }
                        }
                    }
                }

                GlassBottomBar(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )

                mission.confirmation?.let { request ->
                    ConfirmationModal(
                        request = request,
                        onApprove = { vm.confirm(true) },
                        onReject = { vm.confirm(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopHeader(mission: MissionState, modelName: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Zero", style = ZtType.title, color = ZtInk)
        Text("Tap", style = ZtType.title, color = ZtTeal)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(runStatusColor(mission.status), 7)
            Spacer(Modifier.width(8.dp))
            Text(
                if (mission.status == RunStatus.IDLE) modelName else runStatusLabel(mission.status),
                style = ZtType.label,
                color = ZtInkDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlassBottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { item ->
                val active = item == selected
                Row(
                    Modifier
                        .clickableChip { onSelect(item) }
                        .background(
                            if (active) ZtTeal.copy(alpha = 0.16f) else Color.Transparent,
                            CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = if (active) ZtTeal else ZtInkFaint,
                        modifier = Modifier.size(22.dp)
                    )
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Text(item.title, style = ZtType.small, color = ZtTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationModal(
    request: ConfirmationRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickableChip { /* swallow */ },
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            Modifier.fillMaxWidth().padding(28.dp).systemBarsPadding(),
            glow = ZtAmber.copy(alpha = 0.5f)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(ZtAmber.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Warning, null, tint = ZtAmber, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    SectionLabel(request.title, color = ZtAmber)
                }
                Spacer(Modifier.size(18.dp))
                Text(request.summary, style = ZtType.title, color = ZtInk)
                if (request.detail.isNotBlank()) {
                    Spacer(Modifier.size(8.dp))
                    Text(request.detail, style = ZtType.body, color = ZtInkDim)
                }
                Spacer(Modifier.size(24.dp))
                GlowButton(
                    text = "Approve",
                    onClick = onApprove,
                    colors = listOf(ZtAmber, Color(0xFFFFDFA6)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(10.dp))
                GhostButton(text = "Don't do this", onClick = onReject, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
