package com.example.jamuchat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuchat.ChatMessage
import com.example.jamuchat.ui.theme.TelegramBluePrimary

@Composable
fun JamuMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isMe = message.isCurrentUser

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = TelegramBluePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            color = if (isMe) Color(0xFF0284C7) else Color(0xFF1E293B),
            contentColor = Color(0xFFF8FAFC),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (!message.imageUrl.isNullOrBlank() && message.imageUrl != "null" && message.imageUrl != "None") {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Xabar rasmi",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = Color(0xFFF8FAFC)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${message.timestamp} ✓✓",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isMe) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
