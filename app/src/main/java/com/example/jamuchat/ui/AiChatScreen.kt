package com.example.jamuchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuchat.ChatMessage
import com.example.jamuchat.ui.theme.JamuOnlineGreen
import com.example.jamuchat.ui.theme.TelegramBluePrimary

@Composable
fun AiChatScreen(
    serverUrl: String,
    onBack: () -> Unit,
    onSendAiPrompt: (prompt: String, onReply: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var messageText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val aiMessages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "ai_welcome",
                senderName = "JAMU AI",
                text = "Salom! Men JAMU.chat AI Yordamchiman 🤖. Menga har qanday savolingizni berishingiz mumkin. Sizga qanday yordam bera olaman?",
                isCurrentUser = false,
                timestamp = "AI"
            )
        )
    }

    val suggestedQuestions = listOf(
        "Dasturlash nima? 💻",
        "Python va Kotlin farqi 🚀",
        "Yaxshi maslahat ber 💡",
        "Qiziqarli fakt ayt 🧠"
    )

    LaunchedEffect(aiMessages.size, isLoading) {
        if (aiMessages.isNotEmpty()) {
            listState.animateScrollToItem(aiMessages.size - 1)
        }
    }

    fun handleSendPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || isLoading) return

        val userMsgId = java.util.UUID.randomUUID().toString()
        aiMessages.add(
            ChatMessage(
                id = userMsgId,
                senderName = "Siz",
                text = trimmed,
                isCurrentUser = true,
                timestamp = "Hozir"
            )
        )

        messageText = ""
        isLoading = true

        onSendAiPrompt(trimmed) { aiReply ->
            isLoading = false
            val aiMsgId = java.util.UUID.randomUUID().toString()
            aiMessages.add(
                ChatMessage(
                    id = aiMsgId,
                    senderName = "JAMU AI",
                    text = aiReply,
                    isCurrentUser = false,
                    timestamp = "AI"
                )
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("AI muloqotini tozalash?", fontWeight = FontWeight.Bold) },
            text = { Text("Barcha AI savol-javoblar tarixi tozalanadi.") },
            confirmButton = {
                Button(
                    onClick = {
                        aiMessages.clear()
                        aiMessages.add(
                            ChatMessage(
                                id = "ai_welcome",
                                senderName = "JAMU AI",
                                text = "Muloqot tozalandi! Savolingiz bo'lsa beravering 🤖",
                                isCurrentUser = false,
                                timestamp = "AI"
                            )
                        )
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tozalash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    val neonBorder = Brush.horizontalGradient(
        listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFFA855F7))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // AI Header
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, neonBorder, CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JAMU AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(JamuOnlineGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bepul AI (Onlayn)", fontSize = 10.sp, color = JamuOnlineGreen)
                    }
                }

                IconButton(onClick = { showClearDialog = true }) {
                    Text("🗑", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(aiMessages, key = { it.id }) { msg ->
                    if (msg.isCurrentUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
                                color = TelegramBluePrimary,
                                modifier = Modifier.padding(start = 50.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(2.dp, 16.dp, 16.dp, 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 40.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TelegramBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI javob tayyorlamoqda... 💭", fontSize = 11.sp, color = TelegramBluePrimary)
                        }
                    }
                }
            }
        }

        // Suggested Questions Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(suggestedQuestions) { q ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.clickable { handleSendPrompt(q) }
                ) {
                    Text(
                        text = q,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Input Field
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("AI ga savol bering...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = TelegramBluePrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { handleSendPrompt(messageText) })
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { handleSendPrompt(messageText) },
                    enabled = messageText.trim().isNotEmpty() && !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBluePrimary),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("➤", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
