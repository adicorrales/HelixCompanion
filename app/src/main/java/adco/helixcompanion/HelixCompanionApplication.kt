package adco.helixcompanion

import android.app.Application
import adco.helixmidimixer.midi.BleMidiManager

/** Process-wide owner for the app's single BLE MIDI session. */
class HelixCompanionApplication : Application() {
    val bleMidiManager: BleMidiManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BleMidiManager(this)
    }
}
