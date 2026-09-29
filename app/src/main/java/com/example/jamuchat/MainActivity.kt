package com.example.jamuchat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jamuchat.network.ChatWebSocketClient
import com.example.jamuchat.network.UserStatus
import com.example.jamuchat.ui.theme.JamuchatTheme
import com.example.jamuchat.util.NotificationHelper
import com.example.jamuchat.util.UserPreferences
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class Screen {
    Splash,
    Login,
    MainChat
}

class MainActivity : ComponentActivity() {

    private var intentSenderName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        setContent {
            JamuchatTheme {
                val context = LocalContext.current
                val userPrefs = remember { UserPreferences(context) }

                var currentScreen by remember { mutableStateOf(Screen.Splash) }
                var currentUserName by remember { mutableStateOf(userPrefs.getUsername() ?: "") }
                var currentUserDisplayName by remember { mutableStateOf(userPrefs.getDisplayName() ?: currentUserName) }
                var currentUserProfileImageUrl by remember { mutableStateOf<String?>(userPrefs.getProfileImageUrl()) }
                var currentPhoneNumber by remember { mutableStateOf(userPrefs.getPhoneNumber()) }
                var selectedPrivateUser by remember { mutableStateOf<String?>(null) }

                var serverUrl by remember { mutableStateOf(userPrefs.getServerUrl()) }

                var isOnline by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                var activeVideoCallUser by remember { mutableStateOf<String?>(null) }
                var incomingVideoCallUser by remember { mutableStateOf<String?>(null) }
                var videoCallStatus by remember { mutableStateOf("idle") }

                val onlineUsersList = remember { mutableStateListOf<UserStatus>() }
                val privateMessagesMap = remember {
                    mutableStateMapOf<String, MutableList<ChatMessage>>()
                }

                val mediaPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
                    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
                    if (!cameraGranted || !audioGranted) {
                        Toast.makeText(
                            context,
                            "Video qo'ng'iroq uchun Kamera va Mikrofon ruxsatlari kerak!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    val requiredPermissions = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requiredPermissions.add(Manifest.permission.CAMERA)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (requiredPermissions.isNotEmpty()) {
                        mediaPermissionLauncher.launch(requiredPermissions.toTypedArray())
                    }
                }

                LaunchedEffect(intentSenderName) {
                    val sender = intentSenderName
                    if (!sender.isNullOrEmpty()) {
                        selectedPrivateUser = sender
                        intentSenderName = null
                    }
                }

                fun getCurrentTimeStr(): String =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                val wsClient = remember {
                    ChatWebSocketClient(
                        onConnectionStatusChanged = { online ->
                            isOnline = online
                            if (online) {
                                errorMessage = null
                            }
                        },
                        onUserListUpdated = { users ->
                            onlineUsersList.clear()
                            onlineUsersList.addAll(users)
                            
                            val selfStatus = users.find { it.name.trim().equals(currentUserName.trim(), ignoreCase = true) }
                            if (selfStatus != null) {
                                if (selfStatus.displayName.isNotBlank()) {
                                    currentUserDisplayName = selfStatus.displayName
                                    userPrefs.saveDisplayName(selfStatus.displayName)
                                }
                                if (!selfStatus.profileImageUrl.isNullOrBlank()) {
                                    currentUserProfileImageUrl = selfStatus.profileImageUrl
                                    userPrefs.saveProfileImageUrl(selfStatus.profileImageUrl)
                                }
                                if (selfStatus.phoneNumber.isNotBlank()) {
                                    currentPhoneNumber = selfStatus.phoneNumber
                                    userPrefs.savePhoneNumber(selfStatus.phoneNumber)
                                }
                            }
                        },
                        onPrivateMessageReceived = { sender, receiver, messageText, imageUrl, timestamp ->
                            val time = if (timestamp.isNotEmpty()) timestamp else getCurrentTimeStr()
                            val isMe = sender.trim().equals(currentUserName.trim(), ignoreCase = true)
                            val otherUser = if (isMe) receiver else sender

                            val list = privateMessagesMap.getOrPut(otherUser) { mutableStateListOf() }
                            
                            if (list.none { it.text == messageText && it.timestamp == time && it.senderName == sender }) {
                                list.add(
                                    ChatMessage(
                                        senderName = sender,
                                        text = messageText,
                                        imageUrl = imageUrl,
                                        isCurrentUser = isMe,
                                        timestamp = time
                                    )
                                )
                            }

                            if (!isMe && selectedPrivateUser != sender) {
                                NotificationHelper.showNotification(
                                    context = context,
                                    senderName = sender,
                                    messageText = if (imageUrl != null) "🖼 Rasm: $messageText" else messageText
                                )
                            }
                        },
                        onChatHistoryReceived = { targetUser, historyMessages ->
                            val list = privateMessagesMap.getOrPut(targetUser) { mutableStateListOf() }
                            list.clear()
                            list.addAll(historyMessages)
                        },
                        onChatHistoryCleared = { targetUser ->
                            privateMessagesMap[targetUser]?.clear()
                        },
                        onVideoCallOfferReceived = { caller ->
                            incomingVideoCallUser = caller
                        },
                        onVideoCallAnswerReceived = { opponent ->
                            if (activeVideoCallUser?.trim()?.equals(opponent.trim(), ignoreCase = true) == true) {
                                videoCallStatus = "connected"
                            }
                        },
                        onVideoCallRejectedReceived = { opponent ->
                            if (activeVideoCallUser?.trim()?.equals(opponent.trim(), ignoreCase = true) == true) {
                                activeVideoCallUser = null
                                videoCallStatus = "idle"
                                Toast.makeText(context, "$opponent qo'ng'iroqni rad etdi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onVideoCallEndedReceived = { opponent ->
                            if (activeVideoCallUser?.trim()?.equals(opponent.trim(), ignoreCase = true) == true) {
                                activeVideoCallUser = null
                                videoCallStatus = "idle"
                                Toast.makeText(context, "$opponent video muloqotni yakunladi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNameTaken = {
                            currentScreen = Screen.Login
                            userPrefs.clearUsername()
                            currentUserName = ""
                        },
                        onErrorOccurred = { error ->
                            errorMessage = error
                        }
                    )
                }

                // Automatically fetch chat history for active user & all user list participants upon connecting
                LaunchedEffect(isOnline, onlineUsersList.toList()) {
                    if (isOnline) {
                        onlineUsersList.forEach { u ->
                            if (!u.name.trim().equals(currentUserName.trim(), ignoreCase = true)) {
                                wsClient.fetchHistory(u.name)
                            }
                        }
                        selectedPrivateUser?.let { target ->
                            wsClient.fetchHistory(target)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        wsClient.disconnect()
                    }
                }

                fun registerUserHttp(serverUrl: String, username: String, phone: String) {
                    Thread {
                        try {
                            val httpBase = getHttpBaseUrl(serverUrl)
                            val url = "$httpBase/users/register"
                            val json = JSONObject().apply {
                                put("username", username)
                                put("phone_number", phone)
                            }
                            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            val request = Request.Builder().url(url).post(body).build()
                            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
                            client.newCall(request).execute().close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }

                fun connectToServer(name: String, phone: String = "", url: String = serverUrl) {
                    currentUserName = name
                    currentUserDisplayName = name
                    currentPhoneNumber = phone
                    serverUrl = url
                    userPrefs.saveUsername(name)
                    userPrefs.savePhoneNumber(phone)
                    userPrefs.saveServerUrl(url)

                    registerUserHttp(url, name, phone)
                    wsClient.connect(url, name, userPrefs.getDeviceId(), phone)

                    try {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                if (!token.isNullOrEmpty()) {
                                    wsClient.sendFcmToken(token)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (incomingVideoCallUser != null) {
                        val caller = incomingVideoCallUser!!
                        AlertDialog(
                            onDismissRequest = {
                                wsClient.rejectVideoCall(caller)
                                incomingVideoCallUser = null
                            },
                            title = {
                                Text("Kiruvchi Video Qo'ng'iroq 📹", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            },
                            text = {
                                Text("$caller sizga video qo'ng'iroq qilmoqda...", style = MaterialTheme.typography.bodyMedium)
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        wsClient.sendVideoCallAnswer(caller)
                                        activeVideoCallUser = caller
                                        videoCallStatus = "connected"
                                        incomingVideoCallUser = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF00E676)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Qabul qilish", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        wsClient.rejectVideoCall(caller)
                                        incomingVideoCallUser = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Rad etish", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (activeVideoCallUser != null) {
                        val opponent = activeVideoCallUser!!
                        val opponentStatus = onlineUsersList.find { it.name.trim().equals(opponent.trim(), ignoreCase = true) }
                        com.example.jamuchat.ui.VideoCallScreen(
                            targetUserName = opponent,
                            targetProfileImageUrl = opponentStatus?.profileImageUrl,
                            callStatus = videoCallStatus,
                            onEndCall = {
                                wsClient.endVideoCall(opponent)
                                activeVideoCallUser = null
                                videoCallStatus = "idle"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        when (currentScreen) {
                        Screen.Splash -> {
                            SplashScreen(
                                onTimeout = {
                                    currentScreen = Screen.Login
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        Screen.Login -> {
                            LoginScreen(
                                initialUsername = currentUserName,
                                initialPhoneNumber = currentPhoneNumber,
                                initialServerUrl = serverUrl,
                                onLoginSuccess = { userName, userPhone, inputUrl ->
                                    connectToServer(userName, userPhone, inputUrl)
                                    currentScreen = Screen.MainChat
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        Screen.MainChat -> {
                            MainChatScreen(
                                currentUserName = currentUserName,
                                currentUserDisplayName = currentUserDisplayName,
                                currentUserProfileImageUrl = currentUserProfileImageUrl,
                                currentUserPhoneNumber = currentPhoneNumber,
                                usersList = onlineUsersList,
                                selectedPrivateUser = selectedPrivateUser,
                                privateMessagesMap = privateMessagesMap,
                                isOnline = isOnline,
                                errorMessage = errorMessage,
                                onReconnect = {
                                    if (currentUserName.isNotEmpty()) {
                                        connectToServer(currentUserName, currentPhoneNumber, serverUrl)
                                    }
                                },
                                onUserClick = { user ->
                                    selectedPrivateUser = user
                                    wsClient.fetchHistory(user)
                                },
                                onSendMessage = { targetUser, text, imageUrl ->
                                    val time = getCurrentTimeStr()
                                    val msgId = java.util.UUID.randomUUID().toString()

                                    fun doSend(finalImageUrl: String?) {
                                        val list = privateMessagesMap.getOrPut(targetUser) { mutableStateListOf() }
                                        list.add(
                                            ChatMessage(
                                                id = msgId,
                                                senderName = currentUserName,
                                                text = text,
                                                imageUrl = finalImageUrl,
                                                isCurrentUser = true,
                                                timestamp = time
                                            )
                                        )

                                        wsClient.sendPrivateMessage(
                                            msgId = msgId,
                                            sender = currentUserName,
                                            receiver = targetUser,
                                            message = text,
                                            imageUrl = finalImageUrl,
                                            timestamp = time
                                        )
                                    }

                                    if (imageUrl != null && (imageUrl.startsWith("content://") || imageUrl.startsWith("file://"))) {
                                        uploadChatImage(
                                            context = context,
                                            serverUrl = serverUrl,
                                            imageUri = imageUrl,
                                            onSuccess = { remoteUrl ->
                                                doSend(remoteUrl)
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Rasm yuborishda xatolik: $err", Toast.LENGTH_SHORT).show()
                                                doSend(null)
                                            }
                                        )
                                    } else {
                                        doSend(imageUrl)
                                    }
                                },
                                onUpdateProfile = { newName, newImg, newPhone ->
                                    currentUserDisplayName = newName
                                    currentPhoneNumber = newPhone
                                    userPrefs.saveDisplayName(newName)
                                    userPrefs.savePhoneNumber(newPhone)
                                    userPrefs.saveProfileImageUrl(newImg)
                                    if (newImg != null && (newImg.startsWith("content://") || newImg.startsWith("file://"))) {
                                        uploadProfileImage(
                                            context = context,
                                            serverUrl = serverUrl,
                                            imageUri = newImg,
                                            onSuccess = { remoteUrl ->
                                                currentUserProfileImageUrl = remoteUrl
                                                userPrefs.saveProfileImageUrl(remoteUrl)
                                                wsClient.updateProfile(currentUserName, newName, remoteUrl, newPhone)
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Profil rasmini yuklashda xatolik: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        currentUserProfileImageUrl = newImg
                                        userPrefs.saveProfileImageUrl(newImg)
                                        wsClient.updateProfile(currentUserName, newName, newImg, newPhone)
                                    }
                                },
                                onClearHistory = { targetUser ->
                                    wsClient.clearHistory(targetUser)
                                    privateMessagesMap[targetUser]?.clear()
                                },
                                onDeleteUserPermanently = { targetUser ->
                                    wsClient.deleteUserPermanently(targetUser)
                                    privateMessagesMap[targetUser]?.clear()
                                },
                                onCallUser = { phone ->
                                    if (phone.isNotBlank()) {
                                        try {
                                            val normalized = normalizePhoneNumber(phone)
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized"))
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Qo'ng'iroq ilovasini ochib bo'lmadi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onStartVideoCall = { targetUser ->
                                    activeVideoCallUser = targetUser
                                    videoCallStatus = "calling"
                                    wsClient.sendVideoCallOffer(targetUser)
                                },
                                onRefreshChat = { targetUser ->
                                    if (currentUserName.isNotEmpty()) {
                                        if (!isOnline) {
                                            connectToServer(currentUserName, serverUrl)
                                        }
                                        wsClient.fetchHistory(targetUser)
                                        Toast.makeText(context, "Yangi o'zgarishlar va xabarlar yangilandi 🔄", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBackFromPrivateChat = {
                                    selectedPrivateUser = null
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val sender = intent?.getStringExtra("EXTRA_CHAT_SENDER")
        if (!sender.isNullOrEmpty()) {
            intentSenderName = sender
        }
    }
}

fun getHttpBaseUrl(wsOrServerUrl: String): String {
    var formatted = wsOrServerUrl.trim().trimEnd('/')
    if (formatted.startsWith("ws://")) {
        formatted = "http://" + formatted.substring(5)
    } else if (formatted.startsWith("wss://")) {
        formatted = "https://" + formatted.substring(6)
    } else if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
        formatted = "http://$formatted"
    }
    if (formatted.endsWith("/ws")) {
        formatted = formatted.substring(0, formatted.length - 3)
    }
    return formatted.trimEnd('/')
}

fun uploadChatImage(
    context: Context,
    serverUrl: String,
    imageUri: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val httpBase = getHttpBaseUrl(serverUrl)
            val uploadUrl = "$httpBase/chat/upload-image"
            
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image bytes")
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val mediaType = mimeType.toMediaTypeOrNull()
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "chat_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Upload failed with status code ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val json = JSONObject(responseBody)
                val remoteImageUrl = json.getString("image_url")
                Handler(Looper.getMainLooper()).post {
                    onSuccess(remoteImageUrl)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JAMU_UPLOAD", "Error uploading chat image", e)
            Handler(Looper.getMainLooper()).post {
                onError(e.localizedMessage ?: "Upload error")
            }
        }
    }.start()
}

fun uploadProfileImage(
    context: Context,
    serverUrl: String,
    imageUri: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val httpBase = getHttpBaseUrl(serverUrl)
            val uploadUrl = "$httpBase/users/profile-image"
            
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image bytes")
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val mediaType = mimeType.toMediaTypeOrNull()
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "profile_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Upload failed with status code ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val json = JSONObject(responseBody)
                val profileImageUrl = json.getString("profile_image_url")
                Handler(Looper.getMainLooper()).post {
                    onSuccess(profileImageUrl)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JAMU_UPLOAD", "Error uploading profile image", e)
            Handler(Looper.getMainLooper()).post {
                onError(e.localizedMessage ?: "Upload error")
            }
        }
    }.start()
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        com.example.jamuchat.ui.components.JamuLogoBadge(size = 110.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    JamuchatTheme {
        MainScreen()
    }
}
