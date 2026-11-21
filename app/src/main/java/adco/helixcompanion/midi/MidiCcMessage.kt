package adco.helixcompanion.midi

data class MidiCcMessage(
    val channel: Int, // 0..15
    val ccNumber: Int, // 0..127
    val value: Int // 0..127
)
