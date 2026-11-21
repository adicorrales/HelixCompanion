package adco.helixcompanion.midi

import android.content.Context
import adco.helixcompanion.HelixCompanionApplication

/**
 * The single MIDI send gateway for the whole app.
 *
 * Everything that writes to the wire funnels through here:
 *  - Song View's `Midi` (shorthand, delays, song message lists) delegates its
 *    byte level write to [send]
 *  - the transport ([TransportManager]) and the Matrix mixer call [sendCc]
 *
 * Routing keeps the historic priority of Song View's `Midi`, so nothing changes
 * for existing setups:
 *   1. direct GATT (when the fallback enabled it, e.g. budget devices)
 *   2. a local USB MIDI port
 *   3. the process wide shared BLE session
 *
 * The low level local transports belong to Song View's `Midi` instance and are
 * registered through [setFallback]; they are cleared when that activity is
 * destroyed, after which only the shared BLE session remains available.
 */
object MidiSender {

    /**
     * Low level local transport owned by Song View's `Midi`: direct GATT writes
     * and local USB MIDI ports.
     */
    interface Fallback {
        /**
         * Called before every send so the instance can refresh its state (it
         * lazily opens USB ports). Returns true when the message must be
         * written by this instance instead of the shared BLE session.
         */
        fun prefersLocalTransport(): Boolean

        /** Write through direct GATT or the local USB port. Never uses BLE. */
        fun sendLocal(bytes: ByteArray): Boolean
    }

    @Volatile
    private var fallback: Fallback? = null

    @Volatile
    private var appContext: Context? = null

    /** Bind the process wide context. Idempotent; called from the Application. */
    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Register the Song View local transport, or clear it with null. */
    @JvmStatic
    fun setFallback(newFallback: Fallback?) {
        fallback = newFallback
    }

    private val bleMidiManager: BleMidiManager?
        get() = (appContext as? HelixCompanionApplication)?.bleMidiManager

    /**
     * Send raw MIDI bytes. Returns true when a transport accepted them.
     * Empty messages are treated as already sent (historic behaviour).
     */
    @JvmStatic
    fun send(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.isEmpty()) {
            return true
        }
        val local = fallback
        if (local != null && local.prefersLocalTransport()) {
            return local.sendLocal(bytes)
        }
        val ble = bleMidiManager
        if (ble != null && ble.isConnected()) {
            return ble.sendMidiMessage(bytes)
        }
        return local?.sendLocal(bytes) ?: false
    }

    /** Build a Control Change message for a 1 based channel. */
    @JvmStatic
    fun ccBytes(channel: Int, ccNumber: Int, value: Int): ByteArray {
        val channel0 = if (channel in 1..16) channel - 1 else channel.coerceIn(0, 15)
        return byteArrayOf(
            (0xB0 or channel0).toByte(),
            (ccNumber and 0x7F).toByte(),
            (value and 0x7F).toByte()
        )
    }

    /** Send one Control Change on a 1 based channel. */
    @JvmStatic
    fun sendCc(channel: Int, ccNumber: Int, value: Int): Boolean =
        send(ccBytes(channel, ccNumber, value))
}
