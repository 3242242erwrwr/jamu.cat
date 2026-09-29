package com.example.jamuchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuchat.ui.components.JamuLogoBadge
import com.example.jamuchat.ui.theme.JamuchatTheme

@Composable
fun LoginScreen(
    initialUsername: String = "",
    initialServerUrl: String = "https://jamu-cat.onrender.com",
    nameTakenError: String? = null,
    onLoginSuccess: (username: String, serverUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by rememberSaveable { mutableStateOf(initialUsername) }
    var serverUrlInput by rememberSaveable { mutableStateOf(initialServerUrl) }
    var isError by rememberSaveable { mutableStateOf(false) }

    fun submitLogin() {
        val trimmedName = nameInput.trim()
        val trimmedUrl = serverUrlInput.trim()
        if (trimmedName.isNotEmpty()) {
            onLoginSuccess(trimmedName, if (trimmedUrl.isNotEmpty()) trimmedUrl else initialServerUrl)
        } else {
            isError = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JamuLogoBadge(size = 110.dp)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Ismingiz:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (isError) isError = false
                    },
                    placeholder = { Text("Ismingizni kiriting...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError || nameTakenError != null,
                    supportingText = {
                        if (nameTakenError != null) {
                            Text(
                                text = nameTakenError,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (isError && nameInput.isEmpty()) {
                            Text(
                                text = "Iltimos, ismingizni kiriting!",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Server Manzili (Wi-Fi yoki Cloudflare WSS Link):",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    placeholder = { Text("ws://192.168.100.16:8000 yoki wss://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitLogin() })
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { submitLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "KIRISH",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    JamuchatTheme {
        LoginScreen(onLoginSuccess = { _, _ -> })
    }
}
