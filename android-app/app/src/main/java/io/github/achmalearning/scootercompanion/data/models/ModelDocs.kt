package io.github.achmalearning.scootercompanion.data.models

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.achmalearning.scootercompanion.privacy.NetworkGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * A model's PDFs (manual, maintenance passport), downloaded only when the owner taps Download
 * and kept in filesDir/scooter-data/<brand>/<model>/. Nothing is bundled in the APK.
 */
class ModelDocs(private val context: Context) {

    fun file(model: ScooterModel, doc: ModelDoc): File {
        val name = doc.file.substringAfterLast('/').ifBlank { "${doc.id}.pdf" }
        val target = File(context.filesDir, "${ModelCatalog.ROOT}/${model.id}/$name")
        // Agility Companion and Scooter Companion up to 3.0.2x copied the bundled PDFs to the
        // root of filesDir: reuse that copy instead of downloading it again.
        val legacy = File(context.filesDir, name)
        if (!target.exists() && legacy.isFile && doc.bytes > 0 && legacy.length() == doc.bytes) {
            target.parentFile?.mkdirs()
            legacy.renameTo(target)
        }
        return target
    }

    fun isDownloaded(model: ScooterModel, doc: ModelDoc): Boolean = file(model, doc).let { it.isFile && it.length() > 0 }

    fun delete(model: ScooterModel, doc: ModelDoc) {
        file(model, doc).delete()
    }

    /** Where a doc is downloaded from: the manufacturer's site, or this project's repository. */
    fun url(model: ScooterModel, doc: ModelDoc): String = doc.url.ifBlank { "$RAW_BASE/${model.id}/${doc.file}" }

    /**
     * Downloads [doc], checking it is a PDF (and its sha256 when the model gives one) before
     * keeping it. Only from a tap: see [NetworkGuard.userRequested].
     */
    suspend fun download(model: ScooterModel, doc: ModelDoc, onProgress: (Float) -> Unit = {}): Result<File> = NetworkGuard.userRequested {
        withContext(Dispatchers.IO) {
            runCatching {
                val target = file(model, doc)
                target.parentFile?.mkdirs()
                val part = File(target.path + ".part")
                val conn = (URL(url(model, doc)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                }
                try {
                    if (conn.responseCode != 200) throw IOException("Download failed (${conn.responseCode})")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: doc.bytes
                    val digest = MessageDigest.getInstance("SHA-256")
                    conn.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                done += n
                                if (total > 0) onProgress((done.toFloat() / total).coerceAtMost(1f))
                            }
                        }
                    }
                    val magic = part.inputStream().use { String(it.readNBytesCompat(4)) }
                    if (magic != "%PDF") throw IOException("Not a PDF")
                    val sha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (doc.sha256.isNotBlank() && !doc.sha256.equals(sha, ignoreCase = true)) throw IOException("Damaged download")
                    if (!part.renameTo(target)) throw IOException("Could not save the file")
                    target
                } finally {
                    conn.disconnect()
                    part.delete()
                }
            }
        }
    }

    /** Opens a downloaded PDF in the phone's PDF viewer. */
    fun openExternally(model: ScooterModel, doc: ModelDoc): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file(model, doc))
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess

    companion object {
        const val RAW_BASE = "https://raw.githubusercontent.com/achma-learning/Scooter-companion/main/scooter-data"

        private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
            val out = ByteArray(n)
            var read = 0
            while (read < n) {
                val r = read(out, read, n - read)
                if (r < 0) break
                read += r
            }
            return out.copyOf(read)
        }
    }
}
