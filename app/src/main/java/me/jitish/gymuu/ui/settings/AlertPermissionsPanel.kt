package me.jitish.gymuu.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.jitish.gymuu.ui.theme.GymuuBorder
import me.jitish.gymuu.ui.theme.GymuuCard
import me.jitish.gymuu.ui.theme.GymuuMuted

/** Explains why the permissions matter before sending the user to system settings. */
@Composable
internal fun AlertPermissionsPanel() {
    val context = LocalContext.current
    var exactAllowed by remember { mutableStateOf(true) }
    var notificationsAllowed by remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    if (exactAllowed && notificationsAllowed) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GymuuCard)
            .border(1.dp, GymuuBorder, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Rest alerts", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (!notificationsAllowed) {
            Text(
                "Notifications are off, so Gymuu can't alert you when rest ends in the background.",
                color = GymuuMuted,
                fontSize = 14.sp
            )
            PanelButton("ENABLE NOTIFICATIONS") {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                )
            }
        }
        if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Text(
                "Without \"Alarms & reminders\", Android may deliver the rest-done alert minutes late.",
                color = GymuuMuted,
                fontSize = 14.sp
            )
            PanelButton("ALLOW EXACT ALARMS") {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            "package:${context.packageName}".toUri()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { Text(label, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp) }
}
