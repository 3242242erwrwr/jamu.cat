package com.example.jamuchat.util

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("JAMU_CHAT_PREFS", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_SERVER_URL = "KEY_SERVER_URL"
        private const val DEFAULT_SERVER_URL = "https://experiencing-regulations-bands-clara.trycloudflare.com"
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username.trim()).apply()
    }

    fun getUsername(): String? {
        val name = prefs.getString(KEY_USERNAME, null)
        return if (!name.isNullOrBlank()) name else null
    }

    fun saveServerUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_SERVER_URL, trimmed).apply()
        }
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
