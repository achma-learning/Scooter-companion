package io.github.achmalearning.scootercompanion.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps android.graphics.pdf.PdfRenderer for one of the Agility PDFs, once downloaded (see
 * data/models/ModelDocs.kt). [open] throws [NotDownloaded] until then.
 *
 * Not thread-safe by itself; all page rendering is serialized through [mutex] because
 * PdfRenderer.openPage() cannot be called concurrently on the same renderer instance.
 */
class PdfPageRenderer(private val file: File) {

    class NotDownloaded : Exception("Not downloaded yet")

    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null
    private var fd: ParcelFileDescriptor? = null
    private val bitmapCache = LruCache<Int, Bitmap>(12)

    val pageCount: Int
        get() = renderer?.pageCount ?: 0

    suspend fun open() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer != null) return@withLock
            if (!file.isFile || file.length() == 0L) throw NotDownloaded()
            fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd!!)
        }
    }

    /** 1-based page number as shown throughout the UI; converted to PdfRenderer's 0-based index. */
    suspend fun renderPage(page: Int, targetWidthPx: Int): Bitmap = withContext(Dispatchers.IO) {
        bitmapCache.get(page)?.let { return@withContext it }
        mutex.withLock {
            bitmapCache.get(page)?.let { return@withLock it }
            val r = renderer ?: error("PdfPageRenderer not opened")
            r.openPage(page - 1).use { pdfPage ->
                val scale = targetWidthPx.toFloat() / pdfPage.width
                val height = (pdfPage.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(targetWidthPx, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmapCache.put(page, bitmap)
                bitmap
            }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            renderer?.close()
            fd?.close()
            renderer = null
            fd = null
            bitmapCache.evictAll()
        }
    }
}
