package com.abdulbasir.sdplayer.data

import android.content.Context

/** Everything the player screen reads from Settings. */
data class PlayerSettings(
    val speed: Float,
    val subtitleSize: Int,
    val resume: Boolean,
    val autoRotate: Boolean,
    val autoPip: Boolean,
    val seekStepSec: Int,
    val gestures: Boolean,
)

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

    var speed: Float
        get() = prefs.getFloat("set_speed", 1f)
        set(value) = prefs.edit().putFloat("set_speed", value).apply()

    /** 0 small, 1 medium, 2 large, 3 huge. */
    var subtitleSize: Int
        get() = prefs.getInt("set_sub_size", 1)
        set(value) = prefs.edit().putInt("set_sub_size", value).apply()

    var resume: Boolean
        get() = prefs.getBoolean("set_resume", true)
        set(value) = prefs.edit().putBoolean("set_resume", value).apply()

    var autoRotate: Boolean
        get() = prefs.getBoolean("set_auto_rotate", true)
        set(value) = prefs.edit().putBoolean("set_auto_rotate", value).apply()

    var autoPip: Boolean
        get() = prefs.getBoolean("set_auto_pip", true)
        set(value) = prefs.edit().putBoolean("set_auto_pip", value).apply()

    var seekStepSec: Int
        get() = prefs.getInt("set_seek_step", 10)
        set(value) = prefs.edit().putInt("set_seek_step", value).apply()

    var gestures: Boolean
        get() = prefs.getBoolean("set_gestures", true)
        set(value) = prefs.edit().putBoolean("set_gestures", value).apply()

    var grid: Boolean
        get() = prefs.getBoolean("set_grid", false)
        set(value) = prefs.edit().putBoolean("set_grid", value).apply()

    fun playerSettings() = PlayerSettings(speed, subtitleSize, resume, autoRotate, autoPip, seekStepSec, gestures)

    fun links(): List<String> =
        (prefs.getString("links", "") ?: "").split('\n').filter { it.isNotBlank() }

    fun addLink(url: String) {
        val list = (listOf(url) + links().filter { it != url }).take(8)
        prefs.edit().putString("links", list.joinToString("\n")).apply()
    }
}
