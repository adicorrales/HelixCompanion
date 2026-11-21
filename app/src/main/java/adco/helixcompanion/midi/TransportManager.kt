package adco.helixcompanion.midi

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import adco.helixcompanion.helixDataStore
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for the Helix transport.
 *
 * Both transport bars observe this object:
 *  - the Song View toolbar (Java views) through [TransportHost]
 *  - the Matrix `TransportBar` (Compose) through the flows below
 *
 * Responsibilities:
 *  - one state machine: On Cue (0) -> Loaded (1) -> Playing / Paused (2)
 *  - the only place that sends transport CCs:
 *      CC49 prev/next (relative), CC51 play pulse, CC47 return to zero
 *  - one configuration (MIDI channel, play mode, song mode) stored in DataStore
 *    with fallback to Song View's legacy `CurrentPreferences` SharedPreferences
 *  - a 400 ms action lock so a double tap cannot queue two commands
 *
 * Song selection itself (CC10 / CC63 absolute commands) is *not* done here:
 * those are sent from the song list, never derived from prev/next.
 */
object TransportManager {

    // ---------------------------------------------------------------------
    // Shared state, observed by both bars
    // ---------------------------------------------------------------------
    private val DEFAULT_STATUS = STATUS_ON_CUE

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playStatus = MutableStateFlow(DEFAULT_STATUS)
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

    // ---------------------------------------------------------------------
    // The machine itself. The Song View mirrors these through
    // [TransportHost.applyTransportState]; it never writes them back.
    // clickCount: 0 = On Cue, 1 = Loaded, 2 = Playing / Paused
    // ---------------------------------------------------------------------
    @Volatile
    var playing: Boolean = false
        private set

    @Volatile
    var clickCount: Int = 0
        private set

    @Volatile
    var statusText: String = DEFAULT_STATUS
        private set

    // ---------------------------------------------------------------------
    // Host (Song View). Weak so a destroyed activity is never leaked.
    // ---------------------------------------------------------------------
    @Volatile
    private var hostRef: WeakReference<TransportHost>? = null

    private val host: TransportHost?
        get() = hostRef?.get()

    /**
     * Register the Song View as the transport host and push the current state
     * so a recreated activity mirrors it immediately.
     */
    @JvmStatic
    fun attachHost(newHost: TransportHost) {
        hostRef = WeakReference(newHost)
        newHost.applyTransportState(playing, clickCount, statusText)
    }

    /** Unregister the Song View. Safe to call twice or with another host. */
    @JvmStatic
    fun detachHost(oldHost: TransportHost) {
        if (hostRef?.get() === oldHost) {
            hostRef = null
        }
    }

    // ---------------------------------------------------------------------
    // State transitions: the only writer of the machine and of the flows
    // ---------------------------------------------------------------------
    /**
     * Move the transport machine and mirror it into the Song View UI.
     * Every transport state change in the app funnels through here.
     */
    @JvmStatic
    fun setState(playing: Boolean, clickCount: Int, status: String) {
        this.playing = playing
        this.clickCount = clickCount
        this.statusText = status
        _isPlaying.value = playing
        _playStatus.value = status
        host?.applyTransportState(playing, clickCount, status)
    }

    /** Push the song information shown by the Song View toolbar. */
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

    // ---------------------------------------------------------------------
    // Transport actions: the only senders of transport CCs
    // ---------------------------------------------------------------------
    /** Previous song: move the selection back, then CC49:0. */
    @JvmStatic
    fun prev() {
        if (!lock()) return
        host?.moveSongSelection(-1)
        applySelfMoveState()
        sendCc(CC_PREV_NEXT, 0)
    }

    /** Next song: move the selection forward, then CC49:127. */
    @JvmStatic
    fun next() {
        if (!lock()) return
        host?.moveSongSelection(1)
        applySelfMoveState()
        sendCc(CC_PREV_NEXT, 127)
    }

    /**
     * State after CC49: the Helix moved on its own, so no CC10 / CC63 is sent
     * (that was the double move). Per the Helix rules the destination only
     * depends on the song mode: Skip loads the new song immediately, Cue leaves
     * it cued. Playback is started by the user's Play press, not by prev/next.
     */
    private fun applySelfMoveState() {
        if (currentSongMode() == SONG_MODE_SKIP) {
            setState(false, 1, STATUS_LOADED)
        } else {
            setState(false, 0, STATUS_ON_CUE)
        }
    }

    /** Return the current song to zero: CC47:0 (no state change). */
    @JvmStatic
    fun rt0() {
        if (!lock()) return
        sendCc(CC_RT0, 0)
    }

    /**
     * Play pulse: CC51 with the single play-mode machine.
     *
     * Play mode: one press toggles play / pause.
     * Load mode: 1st press loads (Loaded), 2nd press plays, further presses
     * toggle play / pause.
     */
    @JvmStatic
    fun play() {
        if (!lock()) return

        if (currentPlayMode() == PLAY_MODE_PLAY) {
            val nowPlaying = !playing
            setState(
                nowPlaying,
                if (nowPlaying) 2 else 0,
                if (nowPlaying) STATUS_PLAYING else ""
            )
            sendCc(CC_PLAY, if (nowPlaying) 127 else 0)
        } else {
            when (clickCount) {
                0 -> {
                    setState(false, 1, STATUS_LOADED)
                    sendCc(CC_PLAY, 127)
                }
                1 -> {
                    setState(true, 2, STATUS_PLAYING)
                    sendCc(CC_PLAY, 127)
                }
                else -> {
                    val nowPlaying = !playing
                    setState(
                        nowPlaying,
                        if (nowPlaying) 2 else 1,
                        if (nowPlaying) STATUS_PLAYING else STATUS_PAUSED
                    )
                    sendCc(CC_PLAY, if (nowPlaying) 127 else 0)
                }
            }
        }
    }

    /**
     * Deliver a transport CC through the single send gateway (shared BLE
     * session first, Song View's local USB / direct GATT behind it). Local sets
     * never send anything.
     */
    private fun sendCc(ccNumber: Int, value: Int) {
        // Local sets never send MIDI (Project.md, sets category); Helix sets go
        // through the single send gateway shared by the whole app.
        if (!isHelixSet()) return
        MidiSender.sendCc(_transportChannel.value, ccNumber, value)
    }

    /** Whether the active set category is "Helix" (owned by Song View settings). */
    private fun isHelixSet(): Boolean =
        legacyPrefs?.getBoolean(KEY_IS_HELIX_SET, true) ?: true

    // ---------------------------------------------------------------------
    // Feedback from the Helix: one entry point for both delivery paths
    // ---------------------------------------------------------------------
    // With a BLE connection every incoming CC reaches the app twice: once
    // through BleMidiManager's parsed CC flow (Matrix) and once through the
    // forwarded MidiReceiver (Song View, which also posts it twice for CCs on
    // the song channel). The same tuple inside a short window is therefore
    // handled exactly once.
    private const val FEEDBACK_DEDUP_WINDOW_MS = 100L
    private val feedbackHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastFeedbackKey: String? = null

    @Volatile
    private var lastFeedbackAtMs = 0L

    // Set change bookkeeping (written by Song View when it sends CC63).
    @Volatile
    private var waitingForLoadConfirmation = false

    // Set when the Helix reports CC51:1 (song started from zero).
    @Volatile
    private var songStartFlag = false

    /** Tell the manager that a set change is waiting for its CC51 confirmation. */
    @JvmStatic
    fun setWaitingForLoadConfirmation(value: Boolean) {
        waitingForLoadConfirmation = value
    }

    private fun isDuplicateFeedback(channel: Int, ccNumber: Int, value: Int): Boolean {
        val key = "$channel:$ccNumber:$value"
        val now = android.os.SystemClock.elapsedRealtime()
        if (key == lastFeedbackKey && now - lastFeedbackAtMs < FEEDBACK_DEDUP_WINDOW_MS) {
            return true
        }
        lastFeedbackKey = key
        lastFeedbackAtMs = now
        return false
    }

    /**
     * Single transport feedback handler. Returns true when the message was a
     * transport CC (51 play, 49 prev/next, 47 return to zero, 10 song select,
     * 63 set select) and has been consumed, false for anything else.
     */
    @JvmStatic
    fun onFeedback(channel: Int, ccNumber: Int, value: Int): Boolean {
        if (ccNumber != CC_PLAY && ccNumber != CC_PREV_NEXT && ccNumber != CC_RT0 &&
            ccNumber != CC_SONG_SELECT && ccNumber != CC_SET_SELECT
        ) {
            return false
        }
        if (isDuplicateFeedback(channel, ccNumber, value)) {
            return true
        }

        if (ccNumber == CC_SONG_SELECT || ccNumber == CC_PREV_NEXT || ccNumber == CC_SET_SELECT) {
            // A new song selection starts a fresh transport state; CC51:1 is an optional start hint.
            songStartFlag = false
        }
        // Transport feedback also releases the action lock.
        unlock()

        when (ccNumber) {
            CC_SONG_SELECT -> onSongSelectFeedback()
            CC_PLAY -> onPlayFeedback(value)
            // CC49 / CC47 / CC63 only reset the flags handled above.
        }
        return true
    }

    private fun onSongSelectFeedback() {
        val songMode = currentSongMode()
        val playMode = currentPlayMode()
        feedbackHandler.post {
            // Ignore a queued load-state update if the song has since reported its start.
            if (songStartFlag) return@post
            if (songMode == SONG_MODE_SKIP) {
                val playing = playMode == PLAY_MODE_PLAY
                setState(
                    playing,
                    if (playing) 2 else 1,
                    if (playing) STATUS_PLAYING else STATUS_LOADED
                )
            } else {
                setState(false, 0, STATUS_ON_CUE)
            }
        }
    }

    private fun onPlayFeedback(value: Int) {
        if (value == 1) {
            // This is a start-from-zero indication, not a play/pause toggle.
            val wasWaitingForLoadConfirmation = waitingForLoadConfirmation
            waitingForLoadConfirmation = false
            songStartFlag = true
            setState(true, 2, STATUS_PLAYING)
            if (wasWaitingForLoadConfirmation) {
                feedbackHandler.post { host?.selectSong(0, false) }
            }
        } else if (waitingForLoadConfirmation) {
            waitingForLoadConfirmation = false
            // Upon receiving confirmation / feedback of song loaded via CC51, select song 0
            // of set # in app UI ONLY (no CC10 sent).
            feedbackHandler.post {
                host?.selectSong(0, false)
                if (currentPlayMode() == PLAY_MODE_PLAY) {
                    setState(true, 2, STATUS_PLAYING)
                } else {
                    setState(false, 1, STATUS_LOADED)
                }
            }
        } else {
            if (currentPlayMode() == PLAY_MODE_PLAY || songStartFlag) {
                val helixPlaying = value > 0
                if (playing != helixPlaying) {
                    val click = if (songStartFlag) {
                        if (helixPlaying) 2 else 1
                    } else {
                        clickCount
                    }
                    val status = when {
                        helixPlaying -> STATUS_PLAYING
                        songStartFlag -> STATUS_PAUSED
                        else -> ""
                    }
                    setState(helixPlaying, click, status)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Action lock (400 ms) - a double tap must not queue two commands
    // ---------------------------------------------------------------------
    private val lockHandler = Handler(Looper.getMainLooper())
    private val lockRelease = Runnable { locked = false }

    @Volatile
    private var locked = false

    private fun lock(): Boolean {
        if (locked) return false
        locked = true
        lockHandler.removeCallbacks(lockRelease)
        lockHandler.postDelayed(lockRelease, 400)
        return true
    }

    /** Release the action lock; the Helix reporting transport feedback does it. */
    @JvmStatic
    fun unlock() {
        locked = false
        lockHandler.removeCallbacks(lockRelease)
    }

    // ---------------------------------------------------------------------
    // Configuration: DataStore first, legacy Song View prefs as fallback
    // ---------------------------------------------------------------------
    const val PLAY_MODE_PLAY = "Play"
    const val PLAY_MODE_LOAD = "Load"
    const val SONG_MODE_CUE = "Cue"
    const val SONG_MODE_SKIP = "Skip"

    const val STATUS_ON_CUE = "On Cue"
    const val STATUS_LOADED = "Loaded"
    const val STATUS_PLAYING = "Playing"
    const val STATUS_PAUSED = "Paused"

    private const val DEFAULT_CHANNEL = 1
    private const val DEFAULT_PLAY_MODE = PLAY_MODE_LOAD
    private const val DEFAULT_SONG_MODE = SONG_MODE_CUE

    // Legacy SharedPreferences, written by Song View's Helix settings screen.
    private const val PREFS_NAME = "CurrentPreferences"
    private const val KEY_CHANNEL = "helixGlobalMidiChannel"
    private const val KEY_PLAY_MODE = "helixPlayMode"
    private const val KEY_SONG_MODE = "helixSongMode"
    private const val KEY_IS_HELIX_SET = "isHelixSet"

    // Canonical DataStore keys.
    private val DS_CHANNEL = intPreferencesKey("transport_midi_channel")
    private val DS_PLAY_MODE = stringPreferencesKey("song_play_mode")
    private val DS_SONG_MODE = stringPreferencesKey("song_mode")

    private val _transportChannel = MutableStateFlow(DEFAULT_CHANNEL)
    val transportChannel: StateFlow<Int> = _transportChannel.asStateFlow()

    private val _playMode = MutableStateFlow(DEFAULT_PLAY_MODE)
    val playMode: StateFlow<String> = _playMode.asStateFlow()

    private val _songMode = MutableStateFlow(DEFAULT_SONG_MODE)
    val songMode: StateFlow<String> = _songMode.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var legacyPrefs: SharedPreferences? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initialized = false

    /**
     * Bind the configuration sources. Idempotent; called from the Application.
     * The first read is synchronous so the very first transport action already
     * uses the configured values.
     */
    @JvmStatic
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val applicationContext = context.applicationContext
        appContext = applicationContext
        val legacy = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs = legacy

        // The settings screen used to store the channel as a String, which makes
        // every getInt() read fall back to 1. Normalize it once, to an Int.
        if (legacy.all[KEY_CHANNEL] is String) {
            legacy.edit()
                .putInt(KEY_CHANNEL, legacyInt(legacy, KEY_CHANNEL, DEFAULT_CHANNEL))
                .apply()
        }

        _transportChannel.value = legacyInt(legacy, KEY_CHANNEL, DEFAULT_CHANNEL)
        _playMode.value = legacyString(legacy, KEY_PLAY_MODE, DEFAULT_PLAY_MODE)
        _songMode.value = legacyString(legacy, KEY_SONG_MODE, DEFAULT_SONG_MODE)

        scope.launch {
            val store = applicationContext.helixDataStore
            // One time migration: DataStore becomes canonical, the legacy prefs
            // stay as a mirror for the readers that have not been re-pointed yet.
            store.edit { snapshot ->
                if (snapshot[DS_CHANNEL] == null && legacy.contains(KEY_CHANNEL)) {
                    snapshot[DS_CHANNEL] = legacyInt(legacy, KEY_CHANNEL, DEFAULT_CHANNEL)
                }
                if (snapshot[DS_PLAY_MODE] == null && legacy.contains(KEY_PLAY_MODE)) {
                    snapshot[DS_PLAY_MODE] = legacyString(legacy, KEY_PLAY_MODE, DEFAULT_PLAY_MODE)
                }
                if (snapshot[DS_SONG_MODE] == null && legacy.contains(KEY_SONG_MODE)) {
                    snapshot[DS_SONG_MODE] = legacyString(legacy, KEY_SONG_MODE, DEFAULT_SONG_MODE)
                }
            }
            store.data.collect { snapshot ->
                _transportChannel.value =
                    snapshot[DS_CHANNEL] ?: legacyInt(legacy, KEY_CHANNEL, DEFAULT_CHANNEL)
                _playMode.value =
                    snapshot[DS_PLAY_MODE] ?: legacyString(legacy, KEY_PLAY_MODE, DEFAULT_PLAY_MODE)
                _songMode.value =
                    snapshot[DS_SONG_MODE] ?: legacyString(legacy, KEY_SONG_MODE, DEFAULT_SONG_MODE)
            }
        }
    }

    /** Transport MIDI channel, 1 based. */
    @JvmStatic
    fun currentChannel(): Int = _transportChannel.value

    /** Transport MIDI channel, 0 based (for raw CC messages). */
    @JvmStatic
    fun currentChannel0Based(): Int = (_transportChannel.value - 1).coerceIn(0, 15)

    /** [PLAY_MODE_PLAY] or [PLAY_MODE_LOAD]. */
    @JvmStatic
    fun currentPlayMode(): String = _playMode.value

    /** [SONG_MODE_CUE] or [SONG_MODE_SKIP]. */
    @JvmStatic
    fun currentSongMode(): String = _songMode.value

    /** Store the transport channel in DataStore and mirror it to the legacy pref. */
    @JvmStatic
    fun setChannel(channel: Int) {
        val value = channel.coerceIn(1, 16)
        _transportChannel.value = value
        legacyPrefs?.edit()?.putInt(KEY_CHANNEL, value)?.apply()
        writeDataStore { it[DS_CHANNEL] = value }
    }

    /** Store the play mode in DataStore and mirror it to the legacy pref. */
    @JvmStatic
    fun setPlayMode(mode: String) {
        val value = if (mode.contains("Load", ignoreCase = true)) PLAY_MODE_LOAD else PLAY_MODE_PLAY
        _playMode.value = value
        legacyPrefs?.edit()?.putString(KEY_PLAY_MODE, value)?.apply()
        writeDataStore { it[DS_PLAY_MODE] = value }
    }

    /** Store the song mode in DataStore and mirror it to the legacy pref. */
    @JvmStatic
    fun setSongMode(mode: String) {
        val value = if (mode.contains("Skip", ignoreCase = true)) SONG_MODE_SKIP else SONG_MODE_CUE
        _songMode.value = value
        legacyPrefs?.edit()?.putString(KEY_SONG_MODE, value)?.apply()
        writeDataStore { it[DS_SONG_MODE] = value }
    }

    private fun writeDataStore(block: (MutablePreferences) -> Unit) {
        val applicationContext = appContext ?: return
        scope.launch { applicationContext.helixDataStore.edit { block(it) } }
    }

    /** Reads an int pref that may have been stored as a String by old writers. */
    private fun legacyInt(source: SharedPreferences, key: String, fallback: Int): Int =
        when (val value = source.all[key]) {
            is Int -> value
            is String -> value.toIntOrNull() ?: fallback
            else -> fallback
        }

    private fun legacyString(source: SharedPreferences, key: String, fallback: String): String =
        try {
            source.getString(key, fallback) ?: fallback
        } catch (e: ClassCastException) {
            fallback
        }

    // ---------------------------------------------------------------------
    // Message ids
    // ---------------------------------------------------------------------
    private const val CC_SONG_SELECT = 10
    private const val CC_PREV_NEXT = 49
    private const val CC_PLAY = 51
    private const val CC_RT0 = 47
    private const val CC_SET_SELECT = 63
}
