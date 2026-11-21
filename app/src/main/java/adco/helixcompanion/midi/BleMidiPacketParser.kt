package adco.helixcompanion.midi

object BleMidiPacketParser {

    private fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (e: Throwable) {
            println("[$tag] $msg")
        }
    }

    /**
     * Builds an official 5-byte BLE-MIDI Control Change (CC) packet with timestamp header.
     * Header Byte: 0x80 | ((timestamp >> 7) & 0x3F)
     * Timestamp Byte: 0x80 | (timestamp & 0x7F)
     * Status Byte: 0xB0 | ((channel - 1) & 0x0F)
     * CC Byte: ccNumber & 0x7F
     * Value Byte: value & 0x7F
     */
    fun createCcPacket(
        channel: Int,
        ccNumber: Int,
        value: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val timestamp = (timestampMs and 0x1FFF).toInt()
        val headerByte = (0x80 or ((timestamp shr 7) and 0x3F)).toByte()
        val timestampByte = (0x80 or (timestamp and 0x7F)).toByte()
        val ch0 = if (channel in 1..16) channel - 1 else channel.coerceIn(0, 15)
        val statusByte = (0xB0 or ch0).toByte()

        val packet = byteArrayOf(
            headerByte,
            timestampByte,
            statusByte,
            (ccNumber and 0x7F).toByte(),
            (value and 0x7F).toByte()
        )
        logDebug("BleMidiPacketParser", "Created BLE-MIDI CC Packet: channel=$channel (0-based=$ch0), cc=$ccNumber, val=$value, bytes=${packet.joinToString(" ") { "0x%02X".format(it) }}")
        return packet
    }

    /**
     * Parses incoming MIDI packet bytes (supporting raw MIDI, a leading
     * header/timestamp byte, and full BLE-MIDI packets) into [MidiCcMessage] objects.
     */
    fun parse(bytes: ByteArray): List<MidiCcMessage> {
        logDebug("BleMidiPacketParser", "Parsing incoming bytes: ${bytes.joinToString(" ") { "0x%02X".format(it) }}")
        if (bytes.size < 3) return emptyList()

        val messages = mutableListOf<MidiCcMessage>()
        val statusOffset = initialStatusOffset(bytes)
        var i = statusOffset

        var runningStatus: Int? = null

        while (i < bytes.size) {
            var b = bytes[i].toInt() and 0xFF

            // In BLE MIDI, a timestamp byte (bit 7 set) precedes a status byte or running status data
            if (statusOffset > 0 && (b and 0x80) != 0 && i + 1 < bytes.size && (bytes[i + 1].toInt() and 0x80) != 0) {
                i++
                if (i >= bytes.size) break
                b = bytes[i].toInt() and 0xFF
            }

            if ((b and 0x80) != 0) {
                // High bit set: MIDI status byte
                if (b in 0xB0..0xBF) {
                    // Control Change status byte
                    runningStatus = b
                    val channel = b and 0x0F
                    if (i + 2 < bytes.size) {
                        val ccNum = bytes[i + 1].toInt() and 0x7F
                        val ccVal = bytes[i + 2].toInt() and 0x7F
                        messages.add(MidiCcMessage(channel, ccNum, ccVal))
                        i += 3
                    } else {
                        break
                    }
                } else if (b in 0x80..0xAF || b in 0xC0..0xEF) {
                    // Other channel voice status byte
                    runningStatus = b
                    val dataLen = if (b in 0xC0..0xDF) 1 else 2
                    i += 1 + dataLen
                } else {
                    // System Realtime / Common / Header byte
                    i++
                }
            } else {
                // High bit is 0: Data byte. Check running status for Control Change
                if (runningStatus != null && runningStatus in 0xB0..0xBF) {
                    if (i + 1 < bytes.size) {
                        val ccNum = b and 0x7F
                        val ccVal = bytes[i + 1].toInt() and 0x7F
                        val channel = runningStatus and 0x0F
                        messages.add(MidiCcMessage(channel, ccNum, ccVal))
                        i += 2
                    } else {
                        break
                    }
                } else {
                    i++
                }
            }
        }

        logDebug(
            "BleMidiPacketParser",
            "Parsed CC messages: " + messages.joinToString { "ch=${it.channel}, cc=${it.ccNumber}, value=${it.value}" }
        )
        return messages
    }

    /**
     * Finds where the MIDI status byte starts.
     *
     * Android's MIDI output port delivers raw MIDI ([status, data...]), but some
     * Bluetooth stacks prepend the BLE-MIDI header and/or timestamp byte
     * ([header, timestamp, status, data...]). In both cases the data bytes that
     * follow the status are below 0x80, so a high bit in the second byte means the
     * first byte was not the status.
     */
    private fun initialStatusOffset(bytes: ByteArray): Int {
        if (bytes.size >= 2 && (bytes[1].toInt() and 0x80) != 0) {
            return if (bytes.size >= 5 && (bytes[2].toInt() and 0x80) != 0) 2 else 1
        }
        return 0
    }
}
