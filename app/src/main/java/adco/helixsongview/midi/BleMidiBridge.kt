package adco.helixsongview.midi

import android.content.Context
import adco.helixcompanion.HelixCompanionApplication
import adco.helixmidimixer.midi.BleConnectionState
import adco.helixmidimixer.midi.BleDeviceInfo
import adco.helixmidimixer.midi.BleMidiManager
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

/**
 * Song View adapter over the single process-wide BLE MIDI connection.
 *
 * The connection state type lives in [adco.helixmidimixer.midi] and is shared with
 * the Matrix Mixer so both features always report the same connection. There used
 * to be a second copy of these models here, which is what allowed Song View and
 * the mixer to disagree about whether the WIDI was connected.
 */
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
                    BleConnectionState.Disconnected -> {
                        if (!connectionPending) refreshFromMidi()
                    }
                    BleConnectionState.Scanning -> {
                        _connectionState.value = BleConnectionState.Scanning
                    }
                    is BleConnectionState.Connecting -> {
                        _connectionState.value = BleConnectionState.Connecting(state.deviceName)
                    }
                    is BleConnectionState.Connected -> {
                        connectionPending = false
                        connectJob?.cancel()
                        midi.setMidiDeviceName(state.deviceName)
                        midi.setMidiDeviceAddress(state.address)
                        _connectionState.value = BleConnectionState.Connected(state.deviceName, state.address)
                    }
                    is BleConnectionState.Error -> {
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

    private fun BleDeviceInfo.toSongViewDevice() = BleDeviceInfo(
        device = device,
        name = name,
        address = address,
        rssi = rssi
    )

    private fun refreshFromMidi() {
        // Do not ask the legacy Midi object here: it can still hold a device that
        // Android released, which is exactly what made the toolbar show a connection
        // with the WIDI switched off. The shared manager is the single source.
        if (manager.isConnected()) {
            _connectionState.value = BleConnectionState.Connected(
                manager.connectedDeviceName(),
                manager.connectedDeviceAddress() ?: ""
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
                // Abort the shared attempt so it cannot connect later and leave the
                // dialog showing an error while the link is actually up.
                manager.disconnect()
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
