package me.jitish.gymuu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.R
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun CompactIconButton(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(40.dp)) {
        content()
    }
}

@Composable
internal fun GymLogo() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
    }
}

@Composable
internal fun AppIconBadge() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun GymFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.navigationBarsPadding().size(62.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
    }
}

@Composable
internal fun SectionHeading(text: String) {
    Text(text, color = GymMuted, fontSize = 16.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Light)
}

@Composable
internal fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Text(text, color = GymMuted, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White))
}

@Composable
internal fun VerticalRule() {
    Box(Modifier.height(48.dp).width(1.dp).background(GymBorder))
}

internal fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
    }
}
