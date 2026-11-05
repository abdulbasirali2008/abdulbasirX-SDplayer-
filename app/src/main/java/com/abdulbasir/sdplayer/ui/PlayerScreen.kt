package com.abdulbasir.sdplayer.ui

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the player reports, as Compose state. */
private class PlayerUiState(private val player: Player) : Player.Listener {
    var isPlaying by mutableStateOf(player.isPlaying)
    var buffering by mutableStateOf(player.playbackState == Player.STATE_BUFFERING)
    var title by mutableStateOf("")
    var duration by mutableLongStateOf(0L)
    var position by mutableLongStateOf(0L)
    var speed by mutableStateOf(1f)
    var repeatOne by mutableStateOf(false)
    var tracks by mutableStateOf(player.currentTracks)
    var hasNext by mutableStateOf(false)
    var hasPrevious by mutableStateOf(false)
    var revision by mutableIntStateOf(0)

    init {
        sync()
    }

    fun refreshPosition() {
        position = player.currentPosition.coerceAtLeast(0L)
        duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
    }

    private fun sync() {
        isPlaying = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        title = player.mediaMetadata.title?.toString()
            ?: player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
        speed = player.playbackParameters.speed
        repeatOne = player.repeatMode == Player.REPEAT_MODE_ONE
        tracks = player.currentTracks
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()
        revision++
        refreshPosition()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        sync()
    }
}

/** The little message in the middle of the screen while you swipe or double tap. */
private data class Hud(val icon: ImageVector?, val text: String, val progress: Float? = null)

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private enum class FitMode(val label: String, val mode: Int) {
    Fit("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Crop("Crop to fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    Stretch("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
}

@Composable
fun PlayerScreen(
    player: Player,
    inPip: Boolean,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
    onAddSubtitle: (Uri) -> Unit,
    getBrightness: () -> Float,
    setBrightness: (Float) -> Unit,
) {
    val context = LocalContext.current
    val state = remember(player) { PlayerUiState(player) }
    DisposableEffect(player) {
        player.addListener(state)
        onDispose { player.removeListener(state) }
    }
    LaunchedEffect(player) {
        while (true) {
            state.refreshPosition()
            kotlinx.coroutines.delay(250)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var fit by remember { mutableStateOf(FitMode.Fit) }
    var hud by remember { mutableStateOf<Hud?>(null) }
    var showTracks by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, state.isPlaying, interaction, showTracks) {
        if (controlsVisible && state.isPlaying && !showTracks) {
            kotlinx.coroutines.delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(hud) {
        if (hud != null) {
            kotlinx.coroutines.delay(800)
            hud = null
        }
    }

    BackHandler(enabled = locked) { locked = false }

    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        val limit = if (player.duration == C.TIME_UNSET) target else player.duration
        player.seekTo(target.coerceAtMost(limit))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            },
            update = { it.resizeMode = fit.mode },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPip) {
            // Touch layer: tap, double tap, and swipes.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(locked) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interaction++
                            },
                            onDoubleTap = { offset ->
                                if (!locked) {
                                    if (offset.x < size.width / 2f) {
                                        seekBy(-10_000L)
                                        hud = Hud(Icons.Rounded.Replay10, "-10 s")
                                    } else {
                                        seekBy(10_000L)
                                        hud = Hud(Icons.Rounded.Forward10, "+10 s")
                                    }
                                }
                            },
                        )
                    }
                    .pointerInput(locked) {
                        if (locked) return@pointerInput
                        var mode = 0 // 0 undecided, 1 seek, 2 brightness, 3 volume
                        var startX = 0f
                        var totalX = 0f
                        var totalY = 0f
                        var startPosition = 0L
                        var seekTarget = 0L
                        var brightness = 0.5f
                        var volume = 0f
                        detectDragGestures(
                            onDragStart = { offset ->
                                mode = 0
                                startX = offset.x
                                totalX = 0f
                                totalY = 0f
                                startPosition = player.currentPosition
                                seekTarget = startPosition
                                brightness = getBrightness()
                                volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            },
                            onDragEnd = {
                                if (mode == 1) player.seekTo(seekTarget)
                                mode = 0
                            },
                            onDragCancel = { mode = 0 },
                        ) { change, amount ->
                            change.consume()
                            totalX += amount.x
                            totalY += amount.y
                            if (mode == 0 && (abs(totalX) > 30f || abs(totalY) > 30f)) {
                                mode = if (abs(totalX) > abs(totalY)) {
                                    1
                                } else if (startX < size.width / 2f) {
                                    2
                                } else {
                                    3
                                }
                            }
                            when (mode) {
                                1 -> {
                                    val duration = if (player.duration == C.TIME_UNSET) Long.MAX_VALUE else player.duration
                                    // A full swipe across the screen is about 90 seconds.
                                    seekTarget = (startPosition + (totalX / size.width * 90_000f).toLong())
                                        .coerceIn(0L, duration)
                                    val delta = (seekTarget - startPosition) / 1000
                                    val sign = if (delta >= 0) "+" else "-"
                                    hud = Hud(null, "${formatTime(seekTarget)}  ($sign${abs(delta)} s)")
                                }
                                2 -> {
                                    brightness = (brightness - amount.y / size.height * 1.3f).coerceIn(0.02f, 1f)
                                    setBrightness(brightness)
                                    hud = Hud(Icons.Rounded.BrightnessMedium, "${(brightness * 100).roundToInt()}%", brightness)
                                }
                                3 -> {
                                    volume = (volume - amount.y / size.height * maxVolume * 1.3f).coerceIn(0f, maxVolume.toFloat())
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume.roundToInt(), 0)
                                    val fraction = volume / maxVolume
                                    hud = Hud(Icons.Rounded.VolumeUp, "${(fraction * 100).roundToInt()}%", fraction)
                                }
                            }
                        }
                    },
            )

            if (state.buffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                )
            }

            hud?.let { HudView(it, Modifier.align(Alignment.Center)) }

            if (locked) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(start = 20.dp),
                ) {
                    GlassButton(Icons.Rounded.LockOpen, "Unlock", size = 56) {
                        locked = false
                        interaction++
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Controls(
                        state = state,
                        player = player,
                        fit = fit,
                        onBack = onBack,
                        onPip = onPip,
                        onRotate = onRotate,
                        onTracks = { showTracks = true },
                        onLock = {
                            locked = true
                            interaction++
                        },
                        onFit = {
                            val next = FitMode.entries[(fit.ordinal + 1) % FitMode.entries.size]
                            fit = next
                            hud = Hud(Icons.Rounded.AspectRatio, next.label)
                        },
                        onSpeed = { speed ->
                            player.setPlaybackSpeed(speed)
                            hud = Hud(null, "${formatSpeed(speed)}x")
                        },
                        onToggleRepeat = {
                            player.repeatMode = if (state.repeatOne) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                        },
                        onInteract = { interaction++ },
                        onSeek = { target ->
                            player.seekTo(target)
                        },
                        onSeekBy = { seekBy(it) },
                    )
                }
            }

            if (showTracks) {
                TracksDialog(
                    player = player,
                    state = state,
                    onDismiss = { showTracks = false },
                    onAddSubtitle = onAddSubtitle,
                )
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')

@Composable
private fun HudView(hud: Hud, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hud.icon != null) {
            Icon(hud.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
        }
        Text(hud.text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        hud.progress?.let { p ->
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier
                    .width(120.dp)
                    .height(5.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun GlassButton(icon: ImageVector, description: String, size: Int = 44, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    player: Player,
    fit: FitMode,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
    onTracks: () -> Unit,
    onLock: () -> Unit,
    onFit: () -> Unit,
    onSpeed: (Float) -> Unit,
    onToggleRepeat: () -> Unit,
    onInteract: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
) {
    var speedMenu by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    Box(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.Rounded.ArrowBack, "Back") { onBack() }
            Spacer(Modifier.width(12.dp))
            Text(
                text = state.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            GlassButton(Icons.Rounded.Subtitles, "Audio and subtitles") { onTracks() }
            Spacer(Modifier.width(8.dp))
            Box {
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            speedMenu = true
                            onInteract()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${formatSpeed(state.speed)}x", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                    SPEEDS.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${formatSpeed(speed)}x",
                                    color = if (speed == state.speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (speed == state.speed) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                onSpeed(speed)
                                speedMenu = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            GlassButton(Icons.Rounded.PictureInPictureAlt, "Picture in picture") { onPip() }
            Spacer(Modifier.width(8.dp))
            GlassButton(Icons.Rounded.ScreenRotation, "Rotate") { onRotate() }
        }

        // Bottom bar
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            val max = state.duration.coerceAtLeast(1L).toFloat()
            val shown = dragPosition ?: state.position.toFloat()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(shown.toLong()), color = Color.White, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                Slider(
                    value = shown.coerceIn(0f, max),
                    onValueChange = {
                        dragPosition = it
                        onInteract()
                    },
                    onValueChangeFinished = {
                        dragPosition?.let { onSeek(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..max,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatTime(state.duration),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.width(52.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(Icons.Rounded.Lock, "Lock controls") { onLock() }
                GlassButton(
                    if (state.repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repeat",
                ) { onToggleRepeat() }
                Box(Modifier.alpha(if (state.hasPrevious) 1f else 0.35f)) {
                    GlassButton(Icons.Rounded.SkipPrevious, "Previous") {
                        if (state.hasPrevious) player.seekToPreviousMediaItem()
                    }
                }
                GlassButton(Icons.Rounded.Replay10, "Back 10 seconds") {
                    onSeekBy(-10_000L)
                    onInteract()
                }
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                                player.play()
                            }
                            onInteract()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                GlassButton(Icons.Rounded.Forward10, "Forward 10 seconds") {
                    onSeekBy(10_000L)
                    onInteract()
                }
                Box(Modifier.alpha(if (state.hasNext) 1f else 0.35f)) {
                    GlassButton(Icons.Rounded.SkipNext, "Next") {
                        if (state.hasNext) player.seekToNextMediaItem()
                    }
                }
                GlassButton(Icons.Rounded.AspectRatio, "Screen fit: ${fit.label}") { onFit() }
            }
        }
    }
}

private data class TrackChoice(val label: String, val group: Tracks.Group, val index: Int, val selected: Boolean)

private fun trackChoices(tracks: Tracks, type: Int): List<TrackChoice> {
    val result = ArrayList<TrackChoice>()
    var number = 1
    for (group in tracks.groups) {
        if (group.type != type || !group.isSupported) continue
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val language = format.language
                ?.takeIf { it != "und" }
                ?.let { Locale.forLanguageTag(it).displayLanguage }
                ?.takeIf { it.isNotBlank() }
            val name = format.label ?: language ?: "Track $number"
            val extra = if (type == C.TRACK_TYPE_AUDIO && format.channelCount > 0) {
                when (format.channelCount) {
                    1 -> " · mono"
                    2 -> " · stereo"
                    6 -> " · 5.1"
                    8 -> " · 7.1"
                    else -> " · ${format.channelCount} ch"
                }
            } else {
                ""
            }
            result += TrackChoice(name + extra, group, i, group.isTrackSelected(i))
            number++
        }
    }
    return result
}

@Composable
private fun TracksDialog(
    player: Player,
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onAddSubtitle: (Uri) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAddSubtitle(uri)
        onDismiss()
    }
    // Reading these here makes the lists update when a choice is made.
    @Suppress("UNUSED_VARIABLE")
    val revision = state.revision
    val audio = trackChoices(state.tracks, C.TRACK_TYPE_AUDIO)
    val text = trackChoices(state.tracks, C.TRACK_TYPE_TEXT)
    val subtitlesOff = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ||
        text.none { it.selected }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            SectionLabel("Audio", Icons.Rounded.Audiotrack)
            if (audio.isEmpty()) {
                Text(
                    "No audio tracks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            audio.forEach { choice ->
                ChoiceRow(choice.label, choice.selected) {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.index))
                        .build()
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("Subtitles", Icons.Rounded.Subtitles)
            ChoiceRow("Off", subtitlesOff) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            text.forEach { choice ->
                ChoiceRow(choice.label, choice.selected && !subtitlesOff) {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.index))
                        .build()
                }
            }
            TextButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Text("Load subtitle file (.srt, .vtt)")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
    Row(
        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
