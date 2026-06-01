package com.zerotap.app.ui.memory

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotap.app.core.MemoryItem
import com.zerotap.app.core.MemoryKind
import com.zerotap.app.ui.components.GlassSurface
import com.zerotap.app.ui.components.SectionLabel
import com.zerotap.app.ui.components.clickableChip
import com.zerotap.app.ui.theme.ZtAmber
import com.zerotap.app.ui.theme.ZtInk
import com.zerotap.app.ui.theme.ZtInkDim
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtRose
import com.zerotap.app.ui.theme.ZtTeal
import com.zerotap.app.ui.theme.ZtType
import com.zerotap.app.ui.theme.ZtViolet

@Composable
fun MemoryScreen(
    memories: List<MemoryItem>,
    onPin: (MemoryItem) -> Unit,
    onLock: (MemoryItem) -> Unit,
    onDelete: (MemoryItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MemoryKind?>(null) }

    val kinds = remember(memories) { memories.map { it.kind }.distinct() }
    val filtered = memories.filter { item ->
        (filter == null || item.kind == filter) &&
            (query.isBlank() || item.key.contains(query, true) || item.value.contains(query, true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MemoryHeader(total = memories.size) }
        item { SearchBox(query = query, onQuery = { query = it }) }
        item {
            FilterRow(
                kinds = kinds,
                selected = filter,
                onSelect = { filter = it }
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyMemory() }
        } else {
            items(filtered, key = { it.id }) { item ->
                MemoryCard(
                    item = item,
                    onPin = { onPin(item) },
                    onLock = { onLock(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun MemoryHeader(total: Int) {
    GlassSurface(Modifier.fillMaxWidth(), glow = ZtViolet.copy(alpha = 0.4f)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bookmarks, null, tint = ZtViolet, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Memory Center", style = ZtType.title, color = ZtInk)
                Text("$total things I remember about you", style = ZtType.small, color = ZtInkFaint)
            }
        }
    }
}

@Composable
private fun SearchBox(query: String, onQuery: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = ZtInkFaint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search memories", style = ZtType.body, color = ZtInkFaint)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    textStyle = ZtType.body.copy(color = ZtInk),
                    cursorBrush = SolidColor(ZtTeal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilterRow(kinds: List<MemoryKind>, selected: MemoryKind?, onSelect: (MemoryKind?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip("All", selected == null) { onSelect(null) }
        kinds.forEach { kind ->
            FilterChip(kind.name.lowercase().replaceFirstChar { it.uppercase() }, selected == kind) { onSelect(kind) }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clickableChip(onClick)
            .background(
                if (active) ZtTeal.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                CircleShape
            )
            .border(1.dp, if (active) ZtTeal.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, style = ZtType.small, color = if (active) ZtTeal else ZtInkDim)
    }
}

@Composable
private fun MemoryCard(
    item: MemoryItem,
    onPin: () -> Unit,
    onLock: () -> Unit,
    onDelete: () -> Unit
) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(ZtViolet.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(item.kind.name, style = ZtType.label, color = ZtViolet)
                }
                Spacer(Modifier.weight(1f))
                if (item.pinned) Icon(Icons.Rounded.PushPin, null, tint = ZtAmber, modifier = Modifier.size(16.dp))
                if (item.locked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Lock, null, tint = ZtTeal, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(item.key, style = ZtType.section, color = ZtInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(item.value, style = ZtType.body, color = ZtInkDim)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MemoryAction(if (item.pinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin, if (item.pinned) "Unpin" else "Pin", ZtAmber, onPin)
                MemoryAction(if (item.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock, if (item.locked) "Unlock" else "Lock", ZtTeal, onLock)
                Spacer(Modifier.weight(1f))
                MemoryAction(Icons.Rounded.Delete, "Delete", ZtRose, onDelete)
            }
        }
    }
}

@Composable
private fun MemoryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clickableChip(onClick)
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = ZtType.small, color = ZtInkDim)
    }
}

@Composable
private fun EmptyMemory() {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Bookmarks, null, tint = ZtInkFaint, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text("No memories here yet", style = ZtType.section, color = ZtInkDim)
            Spacer(Modifier.height(4.dp))
            Text("ZeroTap learns as you complete tasks.", style = ZtType.small, color = ZtInkFaint)
        }
    }
}
