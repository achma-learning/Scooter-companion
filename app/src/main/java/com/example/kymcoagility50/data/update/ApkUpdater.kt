package com.example.kymcoagility50.data.update

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.kymcoagility50.privacy.NetworkGuard
import kotlinx.coroutines.delay
import java.io.File

/**
 * Downloads a release APK with the system DownloadManager (keeps going if the owner leaves
 * the screen) into this app's own external files dir — no storage permission needed — and
 * hands it to the system installer through a FileProvider. Android itself refuses an APK
 * that isn't signed with the same key as the installed app, so a tampered file can't
 * replace it. Installing over the existing app keeps every bit of local data.
 */
class ApkUpdater(private val context: Context) {

    sealed interface Progress {
        /** [fraction] is null while the size is still unknown. */
        data class Running(val fraction: Float?) : Progress
        data object Done : Progress
        data class Failed(val reason: String) : Progress
    }

    enum class InstallResult { STARTED, NEEDS_PERMISSION, FAILED }

    private val downloads = context.getSystemService(DownloadManager::class.java)

    fun installedVersionCode(): Long =
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    fun installedVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    /** Debug builds have their own package id, so a release APK installs beside them, not over. */
    val isDebugBuild: Boolean get() = context.packageName.endsWith(".debug")

    private fun dir(): File = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_DIR)
    private fun nameFor(update: AppUpdate) = "agility-companion-${update.versionCode}.apk"
    private fun fileFor(update: AppUpdate) = File(dir(), nameFor(update))

    /** True when this exact release was already downloaded completely (no second 69 MB). */
    fun isDownloaded(update: AppUpdate): Boolean = fileFor(update).let { it.exists() && (update.apkBytes <= 0 || it.length() == update.apkBytes) }

    /** Starts the download (only from a tap) and reports progress until it ends. */
    suspend fun download(update: AppUpdate, onProgress: (Progress) -> Unit) {
        dir().listFiles()?.forEach { it.delete() } // older updates and half-finished files
        dir().mkdirs()
        val id = NetworkGuard.userRequested {
            downloads.enqueue(
                DownloadManager.Request(Uri.parse(update.apkUrl))
                    .setTitle("Agility Companion ${update.versionName}")
                    .setMimeType(APK_MIME)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "$UPDATE_DIR/${nameFor(update)}")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            )
        }
        while (true) {
            val state = downloads.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (!c.moveToFirst()) return@use Progress.Failed("Download was cancelled")
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Progress.Done
                    DownloadManager.STATUS_FAILED -> Progress.Failed("Download failed (code ${c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))})")
                    else -> Progress.Running(if (total > 0) done.toFloat() / total else null)
                }
            } ?: Progress.Failed("Download was cancelled")
            onProgress(state)
            if (state !is Progress.Running) return
            delay(500)
        }
    }

    /**
     * Opens the system installer. The first time, Android asks the owner to allow installs
     * from this app; we open that settings page and they tap Install again afterwards.
     */
    fun install(update: AppUpdate): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return if (runCatching { context.startActivity(settings) }.isSuccess) InstallResult.NEEDS_PERMISSION else InstallResult.FAILED
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", fileFor(update))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            InstallResult.STARTED
        } catch (_: ActivityNotFoundException) {
            InstallResult.FAILED
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        /** Must match res/xml/update_paths.xml. */
        private const val UPDATE_DIR = "updates"
    }
}
