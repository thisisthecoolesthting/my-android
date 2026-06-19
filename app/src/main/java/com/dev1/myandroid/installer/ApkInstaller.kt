package com.dev1.myandroid.installer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "apk_download"
private const val NOTIF_ID = 1001

sealed class DownloadState {
    data class Progress(val percent: Int) : DownloadState()
    data class Done(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ApkInstaller(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "APK Downloads",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)
    }

    suspend fun download(
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): DownloadState = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "apk").also { it.mkdirs() }
        val outFile = File(cacheDir, fileName)

        val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading build…")
            .setOngoing(true)
            .setProgress(100, 0, false)

        nm.notify(NOTIF_ID, notifBuilder.build())

        runCatching {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                nm.cancel(NOTIF_ID)
                return@withContext DownloadState.Error("HTTP ${response.code}")
            }

            val body = response.body ?: run {
                nm.cancel(NOTIF_ID)
                return@withContext DownloadState.Error("Empty response body")
            }

            val total = body.contentLength()
            var downloaded = 0L

            outFile.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = (downloaded * 100 / total).toInt()
                            onProgress(pct)
                            nm.notify(NOTIF_ID, notifBuilder
                                .setProgress(100, pct, false)
                                .setContentText("$pct%")
                                .build())
                        }
                    }
                }
            }

            nm.cancel(NOTIF_ID)
            DownloadState.Done(outFile)
        }.getOrElse { e ->
            nm.cancel(NOTIF_ID)
            DownloadState.Error(e.message ?: "Unknown error")
        }
    }

    fun install(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
