package adco.helixcompanion.midi

import android.bluetooth.BluetoothDevice

/**
 * Song View side effects required by [BleMidiBridge].
 *
 * The bridge owns the connection logic (scanning, timeout, the shared process
 * wide manager) but the device bookkeeping and the legacy connect trigger live
 * in Song View's `Midi`, which implements this interface. That keeps the bridge
 * free of any dependency on the Song View package.
 *
 * The toolbar connection indicator is refreshed by `Midi`'s own shared
 * connection listener, so it needs no callback here.
 */
interface BleConnectionHost {

    /** Remember the device that just connected (name/address shown by the UI). */
    fun onBleDeviceConnected(name: String?, address: String?)

    /** Start the actual connection towards [device]. May throw on failure. */
    fun connectBleDevice(device: BluetoothDevice)

    /** Drop the current device before a new attempt or on an explicit disconnect. */
    fun disconnectBleDevice()
}
