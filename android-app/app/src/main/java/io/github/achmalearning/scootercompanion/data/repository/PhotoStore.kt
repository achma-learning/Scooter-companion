package io.github.achmalearning.scootercompanion.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * The owner's scooter photo, kept as one small JPEG in app storage. It is scaled down on the
 * way in so it stays light in backups (base64 inside the JSON save file).
 */
class PhotoStore(private val context: Context) {

    private val file get() = File(context.filesDir, FILE_NAME)

    fun exists(): Boolean = file.exists()

    fun path(): File? = file.takeIf { it.exists() }

    /** Copies the picked image, scaled so its longest side is at most [MAX_SIDE] px. */
    fun saveFrom(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Not an image")
        val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true) else decoded
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        file.writeBytes(out.toByteArray())
        return FILE_NAME
    }

    fun delete() { file.delete() }

    fun toBase64(): String = path()?.let { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }.orEmpty()

    /** Restores a photo from a backup; returns the profile's photo field ("" for none). */
    fun restore(base64: String): String {
        if (base64.isBlank()) { delete(); return "" }
        file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
        return FILE_NAME
    }

    companion object {
        const val FILE_NAME = "scooter-photo.jpg"
        const val MAX_SIDE = 1024
    }
}
