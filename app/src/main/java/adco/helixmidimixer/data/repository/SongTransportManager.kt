package adco.helixmidimixer.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SongTransportManager {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playStatus = MutableStateFlow("On Cue")
    val playStatus: StateFlow<String> = _playStatus.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentKey = MutableStateFlow("")
    val currentKey: StateFlow<String> = _currentKey.asStateFlow()

    private val _currentCapo = MutableStateFlow("")
    val currentCapo: StateFlow<String> = _currentCapo.asStateFlow()

    private val _currentTempo = MutableStateFlow("")
    val currentTempo: StateFlow<String> = _currentTempo.asStateFlow()

    private val _prevTitle = MutableStateFlow("")
    val prevTitle: StateFlow<String> = _prevTitle.asStateFlow()

    private val _nextTitle = MutableStateFlow("")
    val nextTitle: StateFlow<String> = _nextTitle.asStateFlow()

    @Volatile
    var transportActionListener: ((String) -> Unit)? = null

    @JvmStatic
    fun updatePlayState(playing: Boolean, status: String) {
        _isPlaying.value = playing
        _playStatus.value = status
    }

    @JvmStatic
    fun updateSongInfo(
        title: String,
        key: String,
        capo: String,
        tempo: String,
        prev: String,
        next: String
    ) {
        _currentTitle.value = title
        _currentKey.value = key
        _currentCapo.value = capo
        _currentTempo.value = tempo
        _prevTitle.value = prev
        _nextTitle.value = next
    }

    @JvmStatic
    fun requestAction(action: String) {
        transportActionListener?.invoke(action)
    }
}
