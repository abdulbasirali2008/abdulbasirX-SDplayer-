package com.abdulbasir.sdplayer.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Preview pictures for the video list, kept in a small memory cache. */
object ThumbLoader {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(context: Context, uri: Uri, width: Int, height: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "$uri#$width"
            cache.get(key)?.let { return@withContext it }
            val bitmap = fromSystem(context, uri, width, height) ?: fromFrame(context, uri)
            if (bitmap != null) cache.put(key, bitmap)
            bitmap
        }

    private fun fromSystem(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        } catch (e: Exception) {
            null
        }
    }

    private fun fromFrame(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
