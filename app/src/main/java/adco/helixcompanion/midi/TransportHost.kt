package adco.helixcompanion.midi

/**
 * Song View side effects required by [TransportManager].
 *
 * The transport machine lives in [TransportManager]; everything that needs the
 * Song View activity (its MIDI fallbacks, its toolbar and its set/song list)
 * is expressed through this interface. The manager keeps only a weak reference
 * to the host, so a destroyed activity is never leaked and never called back.
 */
interface TransportHost {

    /**
     * Mirror a transport state change into the Song View UI (play/pause icon,
     * status text). Called by the manager on every state transition, from any
     * thread; implementations must hop to the main thread themselves.
     */
    fun applyTransportState(playing: Boolean, clickCount: Int, status: String)

    /**
     * Move the song selection inside the current set by [delta] (-1 = previous,
     * +1 = next). Bounds are enforced by the implementation. This only changes
     * the app side selection; the Helix is moved by the CC49 sent afterwards.
     */
    fun moveSongSelection(delta: Int)

    /**
     * Select a song of the current set by absolute position. [sendMidi] is false
     * for UI-only syncs that must not send CC10 (the MIDI side already moved).
     */
    fun selectSong(position: Int, sendMidi: Boolean)
}
