package me.jitish.gymuu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.R
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun RoutineRow(routine: Routine, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(1.dp, GymBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(routine.name, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        CompactIconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(22.dp)) }
        CompactIconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GymDanger, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun ManageRoutinesAction(icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GymMuted, modifier = Modifier.size(22.dp))
        Text("Manage routines", color = Color(0xFFC8C8C8), fontSize = 20.sp)
    }
}

@Composable
internal fun RoutineDrawer(
    routines: List<Routine>,
    onManageRoutines: () -> Unit,
    onRoutineClick: (Routine) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = GymBlack, drawerContentColor = Color.White, modifier = Modifier.fillMaxHeight().widthIn(max = 320.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIconBadge()
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            DividerLine()
            ManageRoutinesAction(icon = Icons.Default.Edit, onClick = onManageRoutines)
            DividerLine()
            SectionHeading("MY ROUTINES")
            routines.forEach { routine ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onRoutineClick(routine) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text(routine.name, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.drawer_credit),
                color = GymMuted,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
