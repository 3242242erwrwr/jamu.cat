package com.example.jamuchat.ui

import android.view.SurfaceView
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jamuchat.ui.components.JamuAvatar
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.delay
import java.util.Locale

// Public Test Agora RTC App ID
private const val AGORA_APP_ID = "e0e271a3964249a099a8fa07153673c4"

@Composable
fun VideoCallScreen(
    targetUserName: String,
    targetProfileImageUrl: String?,
    callChannelName: String = "jamu_default_channel",
    callStatus: String = "calling", // "calling", "connected", "ended"
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableStateOf(0) }

    // Initialize Agora Engine & Join Channel
    DisposableEffect(callChannelName) {
        val eventHandler = object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                remoteUid = uid
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                if (remoteUid == uid) {
                    remoteUid = null
                }
            }
        }

        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = AGORA_APP_ID
                mEventHandler = eventHandler
            }
            val engine = RtcEngine.create(config).apply {
                enableVideo()
                enableAudio()
                setEnableSpeakerphone(true)
            }

            // Local SurfaceView
            val localView = SurfaceView(context)
            engine.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            localSurfaceView = localView

            // Join Agora Channel
            engine.joinChannel(null, callChannelName.lowercase(), "JAMU_User", 0)
            rtcEngine = engine
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                rtcEngine?.stopPreview()
                rtcEngine?.leaveChannel()
                RtcEngine.destroy()
                rtcEngine = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Remote SurfaceView when remote user joins
    LaunchedEffect(remoteUid) {
        val uid = remoteUid
        val engine = rtcEngine
        if (uid != null && engine != null) {
            val remoteView = SurfaceView(context)
            engine.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            remoteSurfaceView = remoteView
        } else {
            remoteSurfaceView = null
        }
    }

    // Toggle Mic Mute
    LaunchedEffect(isMuted) {
        rtcEngine?.muteLocalAudioStream(isMuted)
    }

    // Toggle Camera On/Off
    LaunchedEffect(isCameraOff) {
        rtcEngine?.muteLocalVideoStream(isCameraOff)
    }

    // Call Duration Counter
    LaunchedEffect(callStatus, remoteUid) {
        if (callStatus == "connected" || remoteUid != null) {
            while (true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
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
        // Fullscreen Remote Stream or Avatar View
        if (remoteSurfaceView != null) {
            AndroidView(
                factory = { remoteSurfaceView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                    text = when {
                        remoteUid != null || callStatus == "connected" -> formatDuration(callDurationSeconds)
                        callStatus == "calling" -> "Chaqirilmoqda... 📹"
                        else -> "Muloqot yakunlandi"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remoteUid != null || callStatus == "connected") Color(0xFF00E676) else Color(0xFF00E5FF)
                )
            }
        }

        // Floating Small Local Camera Preview (Top Right)
        AnimatedVisibility(
            visible = callStatus != "ended" && !isCameraOff,
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
                    .size(width = 110.dp, height = 150.dp)
                    .border(1.5.dp, neonBorder, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (localSurfaceView != null) {
                        AndroidView(
                            factory = { localSurfaceView!! },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("📹 Siz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

                // Switch Camera Toggle
                FloatingActionButton(
                    onClick = { rtcEngine?.switchCamera() },
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
