package com.example.jamuchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuchat.network.UserStatus
import com.example.jamuchat.ui.components.JamuAvatar
import com.example.jamuchat.ui.components.JamuEmptyState
import com.example.jamuchat.ui.components.JamuMessageBubble
import com.example.jamuchat.ui.components.JamuMessageInput
import com.example.jamuchat.ui.components.JamuProfileDialog
import com.example.jamuchat.ui.components.JamuUserItem
import com.example.jamuchat.ui.theme.JamuOfflineRed
import com.example.jamuchat.ui.theme.JamuOnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val imageUrl: String? = null,
    val isCurrentUser: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

@Composable
fun MainChatScreen(
    currentUserName: String,
    currentUserDisplayName: String,
    currentUserProfileImageUrl: String?,
    usersList: List<UserStatus>,
    selectedPrivateUser: String?,
    privateMessagesMap: Map<String, List<ChatMessage>>,
    isOnline: Boolean,
    errorMessage: String?,
    onReconnect: () -> Unit,
    onUserClick: (String) -> Unit,
    onSendMessage: (targetUser: String, text: String, imageUrl: String?) -> Unit,
    onUpdateProfile: (displayName: String, imageUrl: String?) -> Unit,
    onClearHistory: (targetUser: String) -> Unit,
    onBackFromPrivateChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var userToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val hiddenUsers = remember { mutableStateListOf<String>() }

    // If new message arrives from a hidden user, remove them from hiddenUsers so they reappear!
    LaunchedEffect(privateMessagesMap.keys.toList()) {
        for (otherUser in privateMessagesMap.keys) {
            val msgs = privateMessagesMap[otherUser]
            if (!msgs.isNullOrEmpty()) {
                val hasIncoming = msgs.any { !it.isCurrentUser }
                if (hasIncoming && hiddenUsers.any { it.trim().equals(otherUser.trim(), ignoreCase = true) }) {
                    hiddenUsers.removeAll { it.trim().equals(otherUser.trim(), ignoreCase = true) }
                }
            }
        }
    }

    if (showProfileDialog) {
        JamuProfileDialog(
            currentDisplayName = currentUserDisplayName.ifBlank { currentUserName },
            currentProfileImageUrl = currentUserProfileImageUrl,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { newName, newImg ->
                onUpdateProfile(newName, newImg)
                showProfileDialog = false
            }
        )
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = "Chatni o'chirish?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Bu foydalanuvchi '$userToDelete' chatlar ro'yxatidan olib tashlanadi.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = userToDelete
                        if (target != null) {
                            if (!hiddenUsers.contains(target)) {
                                hiddenUsers.add(target)
                            }
                            if (selectedPrivateUser?.trim()?.equals(target.trim(), ignoreCase = true) == true) {
                                onBackFromPrivateChat()
                            }
                        }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("O'chirish", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Bekor qilish")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // EKRAN DOIMO IKKI QISMGA BO'LINGAN (Chap: FOYDALANUVCHILAR 35%, O'ng: CHAT 65%)
    Row(modifier = modifier.fillMaxSize()) {
        // CHAP TOMON (35%): FOYDALANUVCHILAR RO'YXATI (Doimiy turadi)
        UserListScreen(
            currentUserName = currentUserName,
            currentUserDisplayName = currentUserDisplayName,
            currentUserProfileImageUrl = currentUserProfileImageUrl,
            usersList = usersList,
            hiddenUsers = hiddenUsers,
            selectedUser = selectedPrivateUser,
            isOnline = isOnline,
            errorMessage = errorMessage,
            onReconnect = onReconnect,
            onUserClick = onUserClick,
            onOpenProfile = { showProfileDialog = true },
            onUserLongClick = { user -> userToDelete = user },
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
        )

        // Ajratuvchi chiziq
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // O'NG TOMON (65%): CHAT OYNASI yoki EMPTY STATE
        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
        ) {
            if (selectedPrivateUser != null) {
                val pMessages = privateMessagesMap[selectedPrivateUser] ?: emptyList()
                val targetUserStatus = usersList.find { it.name.trim().equals(selectedPrivateUser.trim(), ignoreCase = true) }
                val targetIsOnline = targetUserStatus?.isOnline ?: false
                val targetDisplayName = targetUserStatus?.getEffectiveName() ?: selectedPrivateUser
                val targetProfileImage = targetUserStatus?.profileImageUrl
                val targetLastSeen = targetUserStatus?.lastSeen ?: ""

                PrivateChatScreen(
                    targetUserName = targetDisplayName,
                    targetProfileImageUrl = targetProfileImage,
                    targetLastSeen = targetLastSeen,
                    messagesList = pMessages,
                    isTargetOnline = targetIsOnline,
                    isClientOnline = isOnline,
                    showBackButton = false,
                    onSendMessage = { text, imageUrl ->
                        onSendMessage(selectedPrivateUser, text, imageUrl)
                    },
                    onClearHistory = {
                        onClearHistory(selectedPrivateUser)
                    },
                    onBack = onBackFromPrivateChat
                )
            } else {
                JamuEmptyState(
                    title = "JAMU.chat",
                    subtitle = "Foydalanuvchini tanlang",
                    description = "Chap tomondan foydalanuvchini tanlang va suhbatni boshlang"
                )
            }
        }
    }
}

@Composable
fun UserListScreen(
    currentUserName: String,
    currentUserDisplayName: String,
    currentUserProfileImageUrl: String?,
    usersList: List<UserStatus>,
    hiddenUsers: List<String>,
    selectedUser: String? = null,
    isOnline: Boolean,
    errorMessage: String?,
    onReconnect: () -> Unit,
    onOpenProfile: () -> Unit,
    onUserLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    val displayedUsers = usersList.filter { u ->
        val isSelf = u.name.trim().equals(currentUserName.trim(), ignoreCase = true)
        val isHidden = hiddenUsers.any { it.trim().equals(u.name.trim(), ignoreCase = true) }
        !isSelf && !isHidden
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with Profile Button
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenProfile() }
                            .padding(2.dp)
                    ) {
                        JamuAvatar(
                            name = currentUserDisplayName.ifBlank { currentUserName },
                            imageUrl = currentUserProfileImageUrl,
                            isSelf = true,
                            size = 28.dp,
                            fontSize = 11
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUserDisplayName.ifBlank { currentUserName },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onOpenProfile() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "⋮",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) JamuOnlineGreen else JamuOfflineRed)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) JamuOnlineGreen else JamuOfflineRed
                    )
                }

                if (!isOnline && errorMessage != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Button(
                                onClick = onReconnect,
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Ulanish", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Foydalanuvchilar ro'yxati
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = "FOYDALANUVCHILAR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )

            if (displayedUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yo'q",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedUsers) { userStatus ->
                        val isSelf = userStatus.name.trim().equals(currentUserName.trim(), ignoreCase = true)
                        val isSelected = userStatus.name.trim().equals(selectedUser?.trim(), ignoreCase = true)

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        ) {
                            JamuUserItem(
                                userStatus = userStatus,
                                isSelf = isSelf,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelf) {
                                        onOpenProfile()
                                    } else {
                                        onUserClick(userStatus.name)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelf) {
                                        onUserLongClick(userStatus.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateChatScreen(
    targetUserName: String,
    targetProfileImageUrl: String?,
    targetLastSeen: String,
    messagesList: List<ChatMessage>,
    isTargetOnline: Boolean,
    isClientOnline: Boolean,
    showBackButton: Boolean = false,
    onSendMessage: (text: String, imageUrl: String?) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("Chatni tozalash?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text("Bu foydalanuvchi bilan bo'lgan barcha xabarlar va yozishmalar o'chiriladi.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("O'chirish", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Bekor qilish")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Chat Header with Avatar, Status & Clear Button
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                JamuAvatar(
                    name = targetUserName,
                    imageUrl = targetProfileImageUrl,
                    isOnline = isTargetOnline,
                    size = 36.dp,
                    fontSize = 14
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = targetUserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val statusText = if (isTargetOnline) "🟢 Online" else "🔴 " + com.example.jamuchat.ui.components.formatLastSeen(targetLastSeen)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = if (isTargetOnline) JamuOnlineGreen else JamuOfflineRed
                    )
                }

                IconButton(onClick = { showClearDialog = true }) {
                    Text(
                        text = "🗑",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Messages List or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messagesList.isEmpty()) {
                JamuEmptyState(
                    title = "Salomlashishni boshlang 👋",
                    subtitle = targetUserName,
                    description = "$targetUserName ga birinchi xabaringizni yuboring!"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messagesList, key = { it.id }) { message ->
                        JamuMessageBubble(message = message)
                    }
                }
            }
        }

        // Message Input
        JamuMessageInput(
            isOnline = isClientOnline,
            onSendMessage = onSendMessage
        )
    }
}
