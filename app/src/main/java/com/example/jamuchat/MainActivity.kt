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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jamuchat.network.ChatWebSocketClient
import com.example.jamuchat.network.UserStatus
import com.example.jamuchat.ui.theme.JamuchatTheme
import com.example.jamuchat.util.ApkUpdateManager
import com.example.jamuchat.util.NotificationHelper
import com.example.jamuchat.util.UserPreferences
import com.example.jamuchat.util.VersionInfo
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
                var currentUserDisplayName by remember { mutableStateOf(currentUserName) }
                var currentUserProfileImageUrl by remember { mutableStateOf<String?>(null) }
                var selectedPrivateUser by remember { mutableStateOf<String?>(null) }

                var serverUrl by remember { mutableStateOf(userPrefs.getServerUrl()) }

                var isOnline by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                var updateVersionInfo by remember { mutableStateOf<VersionInfo?>(null) }
                var isDownloadingApk by remember { mutableStateOf(false) }
                var apkDownloadProgress by remember { mutableStateOf(0) }

                val onlineUsersList = remember { mutableStateListOf<UserStatus>() }
                val privateMessagesMap = remember {
                    mutableStateMapOf<String, MutableList<ChatMessage>>()
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            "JAMU.chat xabarlar haqida bildirishnoma yuborishi uchun ruxsat kerak",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(serverUrl) {
                    if (serverUrl.isNotBlank()) {
                        ApkUpdateManager.checkVersion(serverUrl) { info ->
                            if (info != null) {
                                val currentVersionCode = ApkUpdateManager.getCurrentVersionCode(context)
                                if (info.versionCode > currentVersionCode && info.apkUrl.isNotBlank()) {
                                    updateVersionInfo = info
                                }
                            }
                        }
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
                                }
                                currentUserProfileImageUrl = selfStatus.profileImageUrl
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

                fun connectToServer(name: String, url: String) {
                    currentUserName = name
                    currentUserDisplayName = name
                    serverUrl = url
                    userPrefs.saveUsername(name)
                    userPrefs.saveServerUrl(url)
                    wsClient.connect(url, name, userPrefs.getDeviceId())

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
                    if (updateVersionInfo != null) {
                        val info = updateVersionInfo!!
                        if (isDownloadingApk) {
                            AlertDialog(
                                onDismissRequest = { },
                                title = { Text("Yangilanmoqda...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { apkDownloadProgress / 100f },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("$apkDownloadProgress%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                },
                                confirmButton = { },
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!info.forceUpdate) {
                                        updateVersionInfo = null
                                    }
                                },
                                title = {
                                    Text(
                                        text = if (info.forceUpdate) "JAMU.chat'ni yangilash kerak." else "Yangi versiya mavjud",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                text = {
                                    Text(
                                        text = "JAMU.chat ${info.versionName} versiyasi mavjud.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            isDownloadingApk = true
                                            apkDownloadProgress = 0
                                            ApkUpdateManager.downloadApk(
                                                context = context,
                                                apkUrl = info.apkUrl,
                                                onProgress = { progress ->
                                                    apkDownloadProgress = progress
                                                },
                                                onComplete = { downloadedFile ->
                                                    isDownloadingApk = false
                                                    if (downloadedFile != null) {
                                                        updateVersionInfo = null
                                                        ApkUpdateManager.installApk(context, downloadedFile)
                                                    } else {
                                                        Toast.makeText(context, "Yuklab olishda xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Yangilash", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    if (!info.forceUpdate) {
                                        TextButton(onClick = { updateVersionInfo = null }) {
                                            Text("Keyinroq")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

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
                                initialServerUrl = serverUrl,
                                onLoginSuccess = { userName, inputUrl ->
                                    connectToServer(userName, inputUrl)
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
                                usersList = onlineUsersList,
                                selectedPrivateUser = selectedPrivateUser,
                                privateMessagesMap = privateMessagesMap,
                                isOnline = isOnline,
                                errorMessage = errorMessage,
                                onReconnect = {
                                    if (currentUserName.isNotEmpty()) {
                                        connectToServer(currentUserName, serverUrl)
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
                                onUpdateProfile = { newName, newImg ->
                                    currentUserDisplayName = newName
                                    if (newImg != null && (newImg.startsWith("content://") || newImg.startsWith("file://"))) {
                                        uploadProfileImage(
                                            context = context,
                                            serverUrl = serverUrl,
                                            imageUri = newImg,
                                            onSuccess = { remoteUrl ->
                                                currentUserProfileImageUrl = remoteUrl
                                                wsClient.updateProfile(currentUserName, newName, remoteUrl)
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Profil rasmini yuklashda xatolik: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        currentUserProfileImageUrl = newImg
                                        wsClient.updateProfile(currentUserName, newName, newImg)
                                    }
                                },
                                onClearHistory = { targetUser ->
                                    wsClient.clearHistory(targetUser)
                                    privateMessagesMap[targetUser]?.clear()
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
