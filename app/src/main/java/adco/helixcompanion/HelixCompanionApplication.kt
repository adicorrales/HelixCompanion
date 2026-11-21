package adco.helixcompanion

import android.app.Application
import adco.helixcompanion.midi.BleMidiManager
import adco.helixcompanion.midi.MidiSender
import adco.helixcompanion.midi.TransportManager

/** Process-wide owner for the app's single BLE MIDI session. */
class HelixCompanionApplication : Application() {
    val bleMidiManager: BleMidiManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BleMidiManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Single send gateway for the whole app (Song View, Matrix, transport).
        MidiSender.init(this)
        // Single source of truth for the transport (both transport bars use it).
        TransportManager.init(this)
    }
}
