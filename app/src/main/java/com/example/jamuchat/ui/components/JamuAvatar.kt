package com.example.jamuchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuchat.ui.theme.JamuOfflineRed
import com.example.jamuchat.ui.theme.JamuOnlineGreen
import com.example.jamuchat.ui.theme.JamuPrimary
import com.example.jamuchat.ui.theme.JamuSecondary

@Composable
fun JamuAvatar(
    name: String,
    imageUrl: String? = null,
    isOnline: Boolean? = null,
    isSelf: Boolean = false,
    isSelected: Boolean = false,
    size: Dp = 40.dp,
    fontSize: Int = 15,
    modifier: Modifier = Modifier
) {
    val neonGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF3B82F6),
            Color(0xFFA855F7),
            Color(0xFF00E5FF)
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isSelected) Modifier
                    .border(2.5.dp, neonGradient, CircleShape)
                    .padding(2.dp)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank() && imageUrl != "null") {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profil rasmi",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isSelf) listOf(JamuPrimary, JamuSecondary)
                            else listOf(JamuSecondary, JamuPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isOnline != null && !isSelf) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(if (isOnline) JamuOnlineGreen else JamuOfflineRed)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
