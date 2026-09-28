package com.example.jamuchat.network

import android.util.Log
import com.example.jamuchat.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class JamuFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("JAMU_FCM", "Yangi FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("JAMU_FCM", "FCM Xabar keldi: ${remoteMessage.data}")

        val sender = remoteMessage.data["sender"]
            ?: remoteMessage.notification?.title
            ?: "JAMU.chat"

        val message = remoteMessage.data["message"]
            ?: remoteMessage.notification?.body
            ?: ""

        if (message.isNotEmpty()) {
            NotificationHelper.showNotification(
                context = applicationContext,
                senderName = sender,
                messageText = message
            )
        }
    }
}
