package com.example.jamuchat.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuchat.network.UserStatus
import com.example.jamuchat.ui.theme.JamuOfflineRed
import com.example.jamuchat.ui.theme.JamuOnlineGreen
import com.example.jamuchat.ui.theme.TelegramBluePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatLastSeen(timestampStr: String): String {
    if (timestampStr.isBlank()) return "Yaqinda kirdi"
    return try {
        val ts = timestampStr.toLong()
        val date = Date(ts)
        val today = Date()
        val formatTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formatDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        if (formatDate.format(date) == formatDate.format(today)) {
            "Bugun, ${formatTime.format(date)} da"
        } else {
            "${SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)}, ${formatTime.format(date)} da"
        }
    } catch (e: Exception) {
        "Yaqinda kirdi"
    }
}

@Composable
fun JamuUserItem(
    userStatus: UserStatus,
    isSelf: Boolean,
    isTyping: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = userStatus.getEffectiveName()

    val neonGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF3B82F6),
            Color(0xFFA855F7),
            Color(0xFF00E5FF)
        )
    )

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isSelf -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(1.5.dp, neonGradient, RoundedCornerShape(10.dp))
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JamuAvatar(
                name = displayName,
                imageUrl = userStatus.profileImageUrl,
                isOnline = userStatus.isOnline,
                isSelf = isSelf,
                isSelected = isSelected,
                size = 32.dp,
                fontSize = 13
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) "$displayName (Siz)" else displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected || isSelf) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                val statusText = if (isSelf) {
                    "Profil"
                } else if (isTyping) {
                    "✍️ Senga xabar yozyapti..."
                } else if (userStatus.isOnline) {
                    "🟢 Online"
                } else {
                    "🔴 " + formatLastSeen(userStatus.lastSeen)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Normal,
                    color = if (isTyping) TelegramBluePrimary else if (userStatus.isOnline) JamuOnlineGreen else JamuOfflineRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
