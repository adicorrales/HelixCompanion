package adco.helixsongview.midi

import android.bluetooth.BluetoothDevice
import android.content.Context
import adco.helixcompanion.HelixCompanionApplication
import adco.helixmidimixer.midi.BleConnectionState as SharedBleConnectionState
import adco.helixmidimixer.midi.BleMidiManager
import adco.helixmidimixer.midi.BleDeviceInfo as SharedBleDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data object Scanning : BleConnectionState
    data class Connecting(val deviceName: String?) : BleConnectionState
    data class Connected(val deviceName: String?, val address: String) : BleConnectionState
    data class Error(val message: String) : BleConnectionState
}

data class BleDeviceInfo(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int
)

/** Song View adapter over the single process-wide BLE MIDI connection. */
class BleMidiBridge(
    context: Context,
    private val midi: Midi
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }

    private val manager: BleMidiManager =
        (context.applicationContext as HelixCompanionApplication).bleMidiManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connectJob: Job? = null
    private var connectionPending = false

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = _scannedDevices.asStateFlow()

    init {
        scope.launch {
            manager.connectionState.collect { state ->
                when (state) {
                    SharedBleConnectionState.Disconnected -> {
                        if (!connectionPending) refreshFromMidi()
                    }
                    SharedBleConnectionState.Scanning -> {
                        _connectionState.value = BleConnectionState.Scanning
                    }
                    is SharedBleConnectionState.Connecting -> {
                        _connectionState.value = BleConnectionState.Connecting(state.deviceName)
                    }
                    is SharedBleConnectionState.Connected -> {
                        connectionPending = false
                        connectJob?.cancel()
                        midi.setMidiDeviceName(state.deviceName)
                        midi.setMidiDeviceAddress(state.address)
                        _connectionState.value = BleConnectionState.Connected(state.deviceName, state.address)
                    }
                    is SharedBleConnectionState.Error -> {
                        connectionPending = false
                        connectJob?.cancel()
                        _connectionState.value = BleConnectionState.Error(state.message)
                    }
                }
            }
        }
        scope.launch {
            manager.scannedDevices.collect { devices ->
                _scannedDevices.value = devices.map { it.toSongViewDevice() }
            }
        }
        refreshFromMidi()
    }

    private fun SharedBleDeviceInfo.toSongViewDevice() = BleDeviceInfo(
        device = device,
        name = name,
        address = address,
        rssi = rssi
    )

    private fun refreshFromMidi() {
        if (midi.hasActiveConnection()) {
            _connectionState.value = BleConnectionState.Connected(
                midi.midiDeviceName,
                midi.midiDeviceAddress
            )
        } else {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    fun startScan() {
        manager.startScan()
    }

    fun stopScan() {
        manager.stopScan()
    }

    fun connect(info: BleDeviceInfo) {
        stopScan()
        connectJob?.cancel()
        midi.disconnectDevice()

        connectionPending = true
        _connectionState.value = BleConnectionState.Connecting(info.name)
        connectJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (connectionPending) {
                connectionPending = false
                _connectionState.value = BleConnectionState.Error(
                    "Connection timed out. The WIDI may be busy or pairing was cancelled."
                )
            }
        }

        try {
            midi.setBluetoothDevice(info.device)
        } catch (e: Exception) {
            connectJob?.cancel()
            connectionPending = false
            _connectionState.value = BleConnectionState.Error("Unable to connect: ${e.message}")
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectionPending = false
        midi.disconnectDevice()
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun release() {
        stopScan()
        connectJob?.cancel()
        scope.cancel()
    }
}
