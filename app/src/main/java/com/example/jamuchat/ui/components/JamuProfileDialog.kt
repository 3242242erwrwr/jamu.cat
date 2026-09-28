package com.example.jamuchat.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JamuProfileDialog(
    currentDisplayName: String,
    currentProfileImageUrl: String?,
    onDismiss: () -> Unit,
    onSaveProfile: (newDisplayName: String, newProfileImageUrl: String?) -> Unit
) {
    var displayNameInput by remember { mutableStateOf(currentDisplayName) }
    var selectedImageUri by remember { mutableStateOf<String?>(currentProfileImageUrl) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profil Sozlamalari",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar Preview
                JamuAvatar(
                    name = if (displayNameInput.isNotBlank()) displayNameInput else "U",
                    imageUrl = selectedImageUri,
                    isSelf = true,
                    size = 80.dp,
                    fontSize = 32
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("📷 Rasm tanlash", fontSize = 11.sp)
                    }

                    if (!selectedImageUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { selectedImageUri = null }
                        ) {
                            Text("🗑 O'chirish", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Taxallusingiz (Nickname):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    placeholder = { Text("Ism yoki Taxallus...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val trimmed = displayNameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onSaveProfile(trimmed, selectedImageUri)
                        }
                    })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = displayNameInput.trim()
                    if (trimmed.isNotEmpty()) {
                        onSaveProfile(trimmed, selectedImageUri)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAQLASH", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
