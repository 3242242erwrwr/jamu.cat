package com.example.jamuchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuchat.ui.components.JamuAvatar
import kotlinx.coroutines.delay

@Composable
fun VideoCallScreen(
    targetUserName: String,
    targetProfileImageUrl: String?,
    callStatus: String = "calling", // "calling", "connected", "ended"
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(callStatus) {
        if (callStatus == "connected") {
            while (true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    val neonBorder = Brush.sweepGradient(
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF3B82F6),
            Color(0xFFA855F7),
            Color(0xFF00E5FF)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Fullscreen Remote User View Container
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, neonBorder, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                JamuAvatar(
                    name = targetUserName,
                    imageUrl = targetProfileImageUrl,
                    size = 120.dp,
                    fontSize = 42
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = targetUserName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callStatus) {
                    "calling" -> "Chaqirilmoqda... 📹"
                    "connected" -> formatDuration(callDurationSeconds)
                    else -> "Muloqot yakunlandi"
                },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (callStatus == "connected") Color(0xFF00E676) else Color(0xFF00E5FF)
            )
        }

        // Floating Small Local Camera Preview (Top Right)
        AnimatedVisibility(
            visible = callStatus != "ended",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(width = 100.dp, height = 140.dp)
                    .border(1.5.dp, neonBorder, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCameraOff) {
                        Text("📷 O'chiq", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📹 Siz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isFrontCamera) "Oldi camera" else "Orqa camera", fontSize = 9.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }

        // Bottom Action Controls Bar
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Toggle
                FloatingActionButton(
                    onClick = { isMuted = !isMuted },
                    shape = CircleShape,
                    containerColor = if (isMuted) Color(0xFFEF4444) else Color(0xFF334155),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Text(if (isMuted) "🎙️❌" else "🎙️", fontSize = 20.sp)
                }

                // Camera Toggle
                FloatingActionButton(
                    onClick = { isCameraOff = !isCameraOff },
                    shape = CircleShape,
                    containerColor = if (isCameraOff) Color(0xFFEF4444) else Color(0xFF334155),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Text(if (isCameraOff) "📷❌" else "📹", fontSize = 20.sp)
                }

                // Flip Camera Toggle
                FloatingActionButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    shape = CircleShape,
                    containerColor = Color(0xFF334155),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Text("🔄", fontSize = 20.sp)
                }

                // End Call Red Button
                FloatingActionButton(
                    onClick = onEndCall,
                    shape = CircleShape,
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier.size(60.dp)
                ) {
                    Text("📞🔴", fontSize = 22.sp)
                }
            }
        }
    }
}
