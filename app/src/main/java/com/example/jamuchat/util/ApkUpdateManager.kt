package com.example.jamuchat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.jamuchat.getHttpBaseUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val forceUpdate: Boolean
)

object ApkUpdateManager {

    fun checkVersion(
        serverUrl: String,
        onResult: (VersionInfo?) -> Unit
    ) {
        Thread {
            try {
                val httpBase = getHttpBaseUrl(serverUrl)
                val requestUrl = "$httpBase/version.json"
                val request = Request.Builder()
                    .url(requestUrl)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            val json = JSONObject(bodyString)
                            val info = VersionInfo(
                                versionCode = json.optInt("versionCode", 1),
                                versionName = json.optString("versionName", "1.0"),
                                apkUrl = json.optString("apkUrl", ""),
                                forceUpdate = json.optBoolean("forceUpdate", false)
                            )
                            Handler(Looper.getMainLooper()).post { onResult(info) }
                            return@use
                        }
                    }
                    Handler(Looper.getMainLooper()).post { onResult(null) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { onResult(null) }
            }
        }.start()
    }

    fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit,
        onComplete: (File?) -> Unit
    ) {
        Thread {
            try {
                val request = Request.Builder().url(apkUrl).build()
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Handler(Looper.getMainLooper()).post { onComplete(null) }
                        return@use
                    }

                    val body = response.body ?: run {
                        Handler(Looper.getMainLooper()).post { onComplete(null) }
                        return@use
                    }

                    val totalBytes = body.contentLength()
                    val targetDir = context.externalCacheDir ?: context.cacheDir
                    val apkFile = File(targetDir, "jamuchat_update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(apkFile)

                    val buffer = ByteArray(8 * 1024)
                    var downloadedBytes = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            Handler(Looper.getMainLooper()).post { onProgress(progress) }
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    Handler(Looper.getMainLooper()).post { onComplete(apkFile) }
                }
            } catch (e: Exception) {
                android.util.Log.e("JAMU_UPDATE", "Download APK error", e)
                Handler(Looper.getMainLooper()).post { onComplete(null) }
            }
        }.start()
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("JAMU_UPDATE", "Install APK error", e)
        }
    }

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }
}
