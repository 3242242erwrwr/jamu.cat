package com.example.jamuchat.util

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("JAMU_CHAT_PREFS", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_PHONE_NUMBER = "KEY_PHONE_NUMBER"
        private const val KEY_SERVER_URL = "KEY_SERVER_URL"
        private const val KEY_DEVICE_ID = "KEY_DEVICE_ID"
        private const val DEFAULT_SERVER_URL = "https://jamu-cat.onrender.com"
    }

    fun savePhoneNumber(phone: String) {
        prefs.edit().putString(KEY_PHONE_NUMBER, phone.trim()).apply()
    }

    fun getPhoneNumber(): String {
        return prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
    }

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username.trim()).apply()
    }

    fun getUsername(): String? {
        val name = prefs.getString(KEY_USERNAME, null)
        if (!name.isNullOrBlank()) {
            val lower = name.trim().lowercase()
            if (lower.contains("jamshid") || lower.contains("dilrabo") || lower.contains("jak")) {
                prefs.edit().remove(KEY_USERNAME).apply()
                return null
            }
            return name
        }
        return null
    }

    fun saveServerUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_SERVER_URL, trimmed).apply()
        }
    }

    fun getServerUrl(): String {
        val saved = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        if (saved.contains("trycloudflare.com") || saved.contains("192.168.")) {
            saveServerUrl(DEFAULT_SERVER_URL)
            return DEFAULT_SERVER_URL
        }
        return saved
    }

    fun clearUsername() {
        prefs.edit().remove(KEY_USERNAME).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
