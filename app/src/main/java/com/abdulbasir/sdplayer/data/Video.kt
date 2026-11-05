package com.abdulbasir.sdplayer.data

import android.net.Uri

data class Video(
    val id: Long,
    val title: String,
    val uri: Uri,
    val durationMs: Long,
    val size: Long,
    val folder: String,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
)

data class VideoFolder(val name: String, val videos: List<Video>)

enum class SortMode(val label: String) {
    Newest("Newest first"),
    Name("Name"),
    Largest("Largest first"),
    Longest("Longest first"),
}

fun List<Video>.ordered(mode: SortMode): List<Video> = when (mode) {
    SortMode.Newest -> sortedByDescending { it.dateAdded }
    SortMode.Name -> sortedBy { it.title.lowercase() }
    SortMode.Largest -> sortedByDescending { it.size }
    SortMode.Longest -> sortedByDescending { it.durationMs }
}

/** The list of videos the player is working through, handed from the library to the player screen. */
object PlayQueue {
    var items: List<Video> = emptyList()
    var startIndex: Int = 0

    fun set(list: List<Video>, index: Int) {
        items = list
        startIndex = index
    }
}
