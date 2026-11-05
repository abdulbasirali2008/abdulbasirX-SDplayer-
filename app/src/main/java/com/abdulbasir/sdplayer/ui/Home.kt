package com.abdulbasir.sdplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdulbasir.sdplayer.MainViewModel
import com.abdulbasir.sdplayer.PlayerActivity
import com.abdulbasir.sdplayer.data.PlayQueue
import com.abdulbasir.sdplayer.data.SortMode
import com.abdulbasir.sdplayer.data.Store
import com.abdulbasir.sdplayer.data.ThumbLoader
import com.abdulbasir.sdplayer.data.Video
import com.abdulbasir.sdplayer.data.VideoFolder
import com.abdulbasir.sdplayer.data.ordered

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Videos("Videos", Icons.Rounded.VideoLibrary),
    Folders("Folders", Icons.Rounded.Folder),
    Recent("Recent", Icons.Rounded.History),
}

@Composable
fun HomeApp(vm: MainViewModel) {
    val context = LocalContext.current

    val videos by vm.videos.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val positions by vm.positions.collectAsStateWithLifecycle()
    val recentIds by vm.recents.collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    androidx.compose.runtime.LaunchedEffect(hasPermission) {
        if (hasPermission) vm.load()
    }

    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showStream by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val store = remember { Store(context) }
    var grid by remember { mutableStateOf(store.grid) }

    BackHandler(enabled = openFolder != null && !showSettings) { openFolder = null }

    fun play(list: List<Video>, index: Int) {
        PlayQueue.set(list, index)
        context.startActivity(Intent(context, PlayerActivity::class.java))
    }

    if (showSettings) SettingsScreen(onBack = { showSettings = false }) else Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (openFolder != null) {
                RoundButton(Icons.Rounded.ArrowBack, "Back") { openFolder = null }
                Spacer(Modifier.width(12.dp))
            } else {
                Logo()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = openFolder ?: "SD Player",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (openFolder == null) {
                    Text(
                        text = "${videos.size} videos",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RoundButton(Icons.Rounded.Link, "Play from link") { showStream = true }
            Spacer(Modifier.width(8.dp))
            Box {
                RoundButton(Icons.Rounded.MoreVert, "Menu") { sortMenu = true }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (grid) "List view" else "Grid view") },
                        onClick = {
                            grid = !grid
                            store.grid = grid
                            sortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            sortMenu = false
                            showSettings = true
                        },
                    )
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Sort: ${mode.label}",
                                    color = if (mode == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (mode == sort) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                vm.setSort(mode)
                                sortMenu = false
                            },
                        )
                    }
                }
            }
        }

        // Search
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search videos") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Tabs
        if (openFolder == null) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
            ) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    val selected = index == tabIndex
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { tabIndex = index }
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tab.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // Content
        val needle = query.trim()
        val bottomPad = PaddingValues(bottom = 24.dp)

        if (!hasPermission) {
            Message(
                title = "Allow access to your videos",
                body = "SD Player needs permission to find the videos on your phone.",
                action = { Button(onClick = { launcher.launch(permission) }) { Text("Allow access") } },
            )
        } else if (loading && videos.isEmpty()) {
            Message("Looking for videos", "One moment…")
        } else if (videos.isEmpty()) {
            Message(
                "No videos found",
                "Videos on your phone will show up here. You can also play a video from a link.",
                action = { Button(onClick = { showStream = true }) { Text("Play from link") } },
            )
        } else {
            val sorted = remember(videos, sort) { videos.ordered(sort) }
            val tab = HomeTab.entries[tabIndex]
            val folderName = openFolder

            val shown: List<Video>? = when {
                folderName != null -> sorted.filter { it.folder == folderName }
                tab == HomeTab.Videos -> sorted
                tab == HomeTab.Recent -> recentIds.mapNotNull { id -> videos.firstOrNull { it.id == id } }
                else -> null
            }?.let { list ->
                if (needle.isEmpty()) list else list.filter { it.title.contains(needle, ignoreCase = true) }
            }

            if (shown != null) {
                if (shown.isEmpty()) {
                    Message(
                        if (needle.isNotEmpty()) "No results" else "Nothing here yet",
                        if (needle.isNotEmpty()) "No video matches \"$needle\"." else "Videos you watch will appear here.",
                    )
                } else {
                    if (grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            gridItems(shown, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    watched = positions[video.id] ?: 0L,
                                    onClick = { play(shown, shown.indexOf(video)) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = bottomPad) {
                            items(shown, key = { it.id }) { video ->
                                VideoRow(
                                    video = video,
                                    watched = positions[video.id] ?: 0L,
                                    onClick = { play(shown, shown.indexOf(video)) },
                                )
                            }
                        }
                    }
                }
            } else {
                val folders = remember(videos, needle) {
                    videos.groupBy { it.folder }
                        .map { VideoFolder(it.key, it.value) }
                        .filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) }
                        .sortedBy { it.name.lowercase() }
                }
                if (folders.isEmpty()) {
                    Message("No folders", "No folder matches \"$needle\".")
                } else {
                    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = bottomPad) {
                        items(folders, key = { it.name }) { folder ->
                            FolderRow(folder) {
                                openFolder = folder.name
                                query = ""
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStream) {
        StreamDialog(
            recent = remember { store.links() },
            onDismiss = { showStream = false },
            onPlay = { url ->
                showStream = false
                store.addLink(url)
                context.startActivity(
                    Intent(context, PlayerActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(url)
                    },
                )
            },
        )
    }
}

@Composable
private fun Logo() {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(Accent, AccentAlt))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Thumb(video: Video, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, video.uri) {
        value = ThumbLoader.load(context, video.uri, 320, 180)?.asImageBitmap()
    }
    Box(
        modifier.background(Brush.linearGradient(listOf(Color(0xFF232A3A), Color(0xFF151922)))),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun VideoRow(video: Video, watched: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Thumb(video, Modifier.fillMaxSize())
            Text(
                text = formatTime(video.durationMs),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            if (watched > 0L && video.durationMs > 0L) {
                val fraction = (watched.toFloat() / video.durationMs).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            val quality = qualityLabel(video.width, video.height)
            Text(
                text = listOf(formatSize(video.size), quality).filter { it.isNotEmpty() }.joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = video.folder,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VideoCard(video: Video, watched: Long, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            Thumb(video, Modifier.fillMaxSize())
            Text(
                text = formatTime(video.durationMs),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            if (watched > 0L && video.durationMs > 0L) {
                val fraction = (watched.toFloat() / video.durationMs).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = video.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = listOf(formatSize(video.size), qualityLabel(video.width, video.height)).filter { it.isNotEmpty() }.joinToString(" · "),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderRow(folder: VideoFolder, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${folder.videos.size} videos",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Message(title: String, body: String, action: @Composable (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
private fun StreamDialog(recent: List<String>, onDismiss: () -> Unit, onPlay: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    val valid = url.trim().startsWith("http://") || url.trim().startsWith("https://")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Play from link") },
        text = {
            Column {
                Text(
                    "Paste a direct link to a video (.mp4, .mkv, .m3u8 …).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("Recent links", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    recent.take(5).forEach { link ->
                        Text(
                            text = link,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(link) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPlay(url.trim()) }, enabled = valid) { Text("Play") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
