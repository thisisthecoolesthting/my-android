package com.dev1.myandroid.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dev1.myandroid.MainActivity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "new_build"
private const val NOTIF_ID = 2001
private const val WORK_NAME = "apk_poll"

class PollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = ManifestRepository(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannel(nm)

        val manifest = repo.fetchLatest().getOrNull() ?: return Result.retry()
        val installedCode = repo.installedVersionCode.first()

        if (manifest.versionCode > installedCode) {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("auto_open", true)
            }
            val pi = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("New build ready")
                .setContentText("${manifest.appName} ${manifest.version} — tap to install")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIF_ID, notif)
        }

        return Result.success()
    }

    private fun ensureChannel(nm: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New Builds",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PollWorker>(
                30, TimeUnit.SECONDS,
                15, TimeUnit.SECONDS  // flex window
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
