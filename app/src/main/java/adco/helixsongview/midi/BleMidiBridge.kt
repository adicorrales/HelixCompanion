package adco.helixsongview.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.media.midi.MidiDevice
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Compose-friendly bridge over the existing Java [Midi] class, so the
 * Matrix Mixer style dialog can scan / connect / disconnect BLE MIDI devices
 * through the same connection that the rest of Song View uses.
 */
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

class BleMidiBridge(
    private val context: Context,
    private val midi: Midi
) {

    companion object {
        private const val TAG = "BleMidiBridge"
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scanner: BluetoothLeScanner? = null
    private var pollJob: Job? = null
    private var connectJob: Job? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = _scannedDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val info = BleDeviceInfo(
                device = device,
                name = device.name ?: "Unknown Device",
                address = device.address,
                rssi = result.rssi
            )
            val current = _scannedDevices.value.toMutableList()
            val idx = current.indexOfFirst { it.address == device.address }
            if (idx >= 0) {
                current[idx] = info
            } else {
                current.add(info)
            }
            _scannedDevices.value = current
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code $errorCode")
            _connectionState.value = BleConnectionState.Error("Scan failed: error code $errorCode")
        }
    }

    init {
        refreshFromMidi()
        pollJob = scope.launch {
            while (isActive) {
                delay(500)
                refreshFromMidi()
            }
        }
    }

    private fun refreshFromMidi() {
        if (midi.hasActiveConnection()) {
            val name = midi.midiDeviceName
            val address = midi.midiDeviceAddress
            val current = _connectionState.value
            if (current !is BleConnectionState.Connected ||
                current.deviceName != name ||
                current.address != address
            ) {
                _connectionState.value = BleConnectionState.Connected(name, address)
            }
        } else {
            when (_connectionState.value) {
                is BleConnectionState.Scanning,
                is BleConnectionState.Connecting -> Unit // keep transient states
                else -> _connectionState.value = BleConnectionState.Disconnected
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is disabled")
            return
        }

        val leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            _connectionState.value = BleConnectionState.Error("BLE Scanner not available")
            return
        }

        scanner = leScanner
        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(midi.uuidBle))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            leScanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permissions for scan", e)
            _connectionState.value = BleConnectionState.Error("Bluetooth permission missing")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan", e)
            _connectionState.value = BleConnectionState.Error("Failed to start scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        scanner = null
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(info: BleDeviceInfo) {
        stopScan()
        connectJob?.cancel()

        try {
            midi.disconnectDevice()
        } catch (_: Exception) {
        }

        midi.setOnMidiDeviceReadyListener(object : Midi.OnMidiDeviceReadyListener {
            override fun onReady(device: MidiDevice) {
                _connectionState.value = BleConnectionState.Connected(
                    midi.midiDeviceName,
                    midi.midiDeviceAddress
                )
            }
        })

        midi.setMidiDeviceName(info.name)
        midi.setMidiDeviceAddress(info.address)
        _connectionState.value = BleConnectionState.Connecting(info.name)

        connectJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_connectionState.value is BleConnectionState.Connecting) {
                _connectionState.value = BleConnectionState.Error(
                    "Connection timed out. The WIDI may be busy in another app."
                )
            }
        }

        try {
            midi.setBluetoothDevice(info.device)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start Bluetooth MIDI connection", e)
            connectJob?.cancel()
            _connectionState.value = BleConnectionState.Error("Unable to connect: ${e.message}")
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        try {
            midi.disconnectDevice()
        } catch (_: Exception) {
        }
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun release() {
        stopScan()
        connectJob?.cancel()
        pollJob?.cancel()
        scope.cancel()
    }
}
