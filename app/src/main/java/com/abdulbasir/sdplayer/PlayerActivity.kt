package com.abdulbasir.sdplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.abdulbasir.sdplayer.data.PlayQueue
import com.abdulbasir.sdplayer.data.Store
import com.abdulbasir.sdplayer.data.Video
import com.abdulbasir.sdplayer.ui.PlayerScreen
import com.abdulbasir.sdplayer.ui.SDTheme

class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var store: Store
    private var inPip by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrs
        }
        hideSystemBars()

        val (items, start) = buildQueue()
        if (items.isEmpty()) {
            finish()
            return
        }

        val exo = ExoPlayer.Builder(this, DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { store.addRecent(it) }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // Finished a video and moved on: forget where it stopped.
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    oldPosition.mediaItem?.mediaId?.toLongOrNull()?.let { store.clearPosition(it) }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exo.currentMediaItem?.mediaId?.toLongOrNull()?.let { store.clearPosition(it) }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    requestedOrientation = if (videoSize.width >= videoSize.height) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }
            }
        })

        val firstId = items[start].mediaId.toLongOrNull()
        val resumeAt = firstId?.let { store.position(it) }?.takeIf { it > 3_000L } ?: C.TIME_UNSET
        firstId?.let { store.addRecent(it) }

        exo.setMediaItems(items, start, resumeAt)
        exo.prepare()
        exo.playWhenReady = true

        setContent {
            SDTheme {
                PlayerScreen(
                    player = exo,
                    inPip = inPip,
                    onBack = { finish() },
                    onPip = { enterPip() },
                    onRotate = { toggleOrientation() },
                    onAddSubtitle = { addSubtitle(it) },
                    getBrightness = { currentBrightness() },
                    setBrightness = { setBrightness(it) },
                )
            }
        }
    }

    private fun buildQueue(): Pair<List<MediaItem>, Int> {
        val data = intent?.data
        if (data != null) {
            val title = displayName(data) ?: data.lastPathSegment ?: "Video"
            val item = MediaItem.Builder()
                .setUri(data)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title.substringBeforeLast('.')).build())
                .build()
            return listOf(item) to 0
        }
        val list = PlayQueue.items
        return list.map { it.toMediaItem() } to PlayQueue.startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    private fun Video.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()

    private fun displayName(uri: Uri): String? {
        if (uri.scheme != "content") return null
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addSubtitle(uri: Uri) {
        val exo = player ?: return
        val item = exo.currentMediaItem ?: return
        val name = (displayName(uri) ?: uri.lastPathSegment ?: "").lowercase()
        val mime = when {
            name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            name.endsWith(".ass") || name.endsWith(".ssa") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val index = exo.currentMediaItemIndex
        val position = exo.currentPosition
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        exo.replaceMediaItem(index, item.buildUpon().setSubtitleConfigurations(listOf(subtitle)).build())
        exo.seekTo(index, position)
    }

    private fun toggleOrientation() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun currentBrightness(): Float {
        val value = window.attributes.screenBrightness
        return if (value < 0f) 0.5f else value
    }

    private fun setBrightness(value: Float) {
        val attrs = window.attributes
        attrs.screenBrightness = value.coerceIn(0.02f, 1f)
        window.attributes = attrs
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) return
        val size = player?.videoSize
        val ratio = if (size != null && size.width > 0 && size.height > 0) {
            (size.width.toFloat() / size.height).coerceIn(0.5f, 2.3f)
        } else {
            16f / 9f
        }
        try {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
                    .build(),
            )
        } catch (e: Exception) {
            // Picture-in-picture isn't available on this phone.
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun savePosition() {
        val exo = player ?: return
        val id = exo.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val duration = exo.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return
        val position = exo.currentPosition
        if (position > 5_000L && position < duration - 5_000L) {
            store.setPosition(id, position)
        } else {
            store.clearPosition(id)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        savePosition()
        player?.pause()
    }

    override fun onDestroy() {
        savePosition()
        player?.release()
        player = null
        super.onDestroy()
    }
}
