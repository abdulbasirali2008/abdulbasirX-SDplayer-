package com.abdulbasir.sdplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abdulbasir.sdplayer.data.SortMode
import com.abdulbasir.sdplayer.data.Store
import com.abdulbasir.sdplayer.data.Video
import com.abdulbasir.sdplayer.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sort = MutableStateFlow(store.sort)
    val sort: StateFlow<SortMode> = _sort.asStateFlow()

    /** Where you stopped in each video (milliseconds), for the progress bars. */
    private val _positions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val positions: StateFlow<Map<Long, Long>> = _positions.asStateFlow()

    private val _recents = MutableStateFlow(store.recents())
    val recents: StateFlow<List<Long>> = _recents.asStateFlow()

    fun load() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            try {
                _videos.value = VideoRepository.load(getApplication())
            } catch (e: Exception) {
                _videos.value = emptyList()
            }
            refreshHistory()
            _loading.value = false
        }
    }

    /** Called when coming back from the player. */
    fun refreshHistory() {
        _positions.value = store.positions(_videos.value)
        _recents.value = store.recents()
    }

    fun setSort(mode: SortMode) {
        store.sort = mode
        _sort.value = mode
    }
}
