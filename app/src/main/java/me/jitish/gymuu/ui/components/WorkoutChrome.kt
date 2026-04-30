package me.jitish.gymuu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun WorkoutHeader(
    title: String,
    onMenu: () -> Unit,
    onRename: () -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        CompactIconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(28.dp)) }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRename)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 23.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.Edit, contentDescription = "Rename day", tint = GymMuted, modifier = Modifier.size(16.dp))
        }
        CompactIconButton(onClick = onAddDay) { Icon(Icons.Default.AddCircleOutline, contentDescription = "Add day", tint = Color.White, modifier = Modifier.size(24.dp)) }
        CompactIconButton(onClick = onRemoveDay) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove day", tint = GymDanger, modifier = Modifier.size(24.dp)) }
        CompactIconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = Color.White, modifier = Modifier.size(28.dp)) }
        CompactIconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next day", tint = Color.White, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
internal fun TopTitleBar(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        CompactIconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(title, color = Color.White, fontSize = 24.sp, letterSpacing = 5.sp)
    }
}

@Composable
internal fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(1.dp, GymBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = GymMuted, modifier = Modifier.size(32.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 21.sp),
            modifier = Modifier
                .weight(1f)
                .clearFocusOnKeyboardDismiss(),
            decorationBox = { inner ->
                if (query.isBlank()) Text("Search exercise...", color = Color(0xFF5F5F5F), fontSize = 21.sp)
                inner()
            }
        )
    }
}

@Composable
internal fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color.White else GymCard,
        border = BorderStroke(1.dp, if (selected) Color.White else GymBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                text = label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false
            )
        }
    }
}
