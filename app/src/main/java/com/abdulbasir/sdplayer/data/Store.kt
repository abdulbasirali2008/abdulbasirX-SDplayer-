package com.abdulbasir.sdplayer.data

import android.content.Context

/** Small settings and history kept on the phone: where you stopped, recent videos, sort order. */
class Store(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("sdplayer", Context.MODE_PRIVATE)

    fun position(id: Long): Long = prefs.getLong("pos_$id", 0L)

    fun setPosition(id: Long, positionMs: Long) {
        prefs.edit().putLong("pos_$id", positionMs).apply()
    }

    fun clearPosition(id: Long) {
        if (prefs.contains("pos_$id")) prefs.edit().remove("pos_$id").apply()
    }

    fun positions(videos: List<Video>): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (v in videos) {
            val p = prefs.getLong("pos_${v.id}", 0L)
            if (p > 0L) map[v.id] = p
        }
        return map
    }

    var sort: SortMode
        get() = SortMode.entries.firstOrNull { it.name == prefs.getString("sort", null) } ?: SortMode.Newest
        set(value) {
            prefs.edit().putString("sort", value.name).apply()
        }

    fun recents(): List<Long> =
        (prefs.getString("recents", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }

    fun addRecent(id: Long) {
        val list = (listOf(id) + recents().filter { it != id }).take(30)
        prefs.edit().putString("recents", list.joinToString(",")).apply()
    }
}
