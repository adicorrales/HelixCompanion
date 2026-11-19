package adco.helixmidimixer.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

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

fun interface BleMidiConnectionListener {
    fun onConnectionStateChanged(state: BleConnectionState)
}

open class BleMidiManager(context: Context) {

    companion object {
        private const val TAG = "BleMidiManager"
        private const val MIDI_OPEN_TIMEOUT_MS = 15_000L
        private const val EXISTING_DEVICE_LOOKUP_DELAY_MS = 300L
        private const val STABLE_CONNECTION_MS = 5_000L

        val MIDI_SERVICE_UUID: UUID = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val midiManager: MidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var scanner: BluetoothLeScanner? = null
    private var midiDevice: MidiDevice? = null
    private var midiInputPort: MidiInputPort? = null
    private var midiOutputPort: MidiOutputPort? = null
    private var connectionTimeoutJob: Job? = null
    private var connectionAttempt = 0
    private var connectedDeviceId: Int? = null

    private var isManualDisconnect = false
    private var lastConnectedDevice: BluetoothDevice? = null
    // When the current session was opened. A device that disappears almost
    // immediately was a stale Android MIDI entry, not a real link: retrying it
    // forever only produces a connection/disconnection loop.
    private var connectedAtMs = 0L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _incomingMidiCc = MutableSharedFlow<MidiCcMessage>(extraBufferCapacity = 64)
    val incomingMidiCc: SharedFlow<MidiCcMessage> = _incomingMidiCc.asSharedFlow()
    private val midiReceivers = CopyOnWriteArraySet<MidiReceiver>()
    private val connectionListeners = CopyOnWriteArraySet<BleMidiConnectionListener>()

    fun addConnectionListener(listener: BleMidiConnectionListener) {
        connectionListeners.add(listener)
        listener.onConnectionStateChanged(_connectionState.value)
    }

    fun removeConnectionListener(listener: BleMidiConnectionListener) {
        connectionListeners.remove(listener)
    }

    private fun publishConnectionState(state: BleConnectionState) {
        _connectionState.value = state
        connectionListeners.forEach { listener ->
            listener.onConnectionStateChanged(state)
        }
    }

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            val message = msg.copyOfRange(offset, offset + count)
            parseAndEmitMidiBytes(message)
            midiReceivers.forEach { receiver ->
                try {
                    receiver.send(message, 0, message.size, timestamp)
                } catch (e: IOException) {
                    Log.w(TAG, "Unable to forward incoming MIDI to a feature", e)
                }
            }
        }
    }

    fun addMidiReceiver(receiver: MidiReceiver) {
        midiReceivers.add(receiver)
    }

    fun removeMidiReceiver(receiver: MidiReceiver) {
        midiReceivers.remove(receiver)
    }

    fun isConnected(): Boolean = _connectionState.value is BleConnectionState.Connected

    fun connectedDeviceName(): String? =
        (_connectionState.value as? BleConnectionState.Connected)?.deviceName

    fun connectedDeviceAddress(): String? =
        (_connectionState.value as? BleConnectionState.Connected)?.address

    private val midiDeviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceRemoved(info: MidiDeviceInfo) {
            if (connectedDeviceId != info.id || isManualDisconnect) return

            Log.w(TAG, "Connected MIDI device removed: id=${info.id}")
            val wasStable = connectedAtMs > 0L &&
                android.os.SystemClock.elapsedRealtime() - connectedAtMs > STABLE_CONNECTION_MS
            closeMidiSession()
            publishConnectionState(BleConnectionState.Disconnected)
            // Only retry after a connection that had actually been working. The
            // system exposes a stale BLE MIDI device after the WIDI is switched off
            // and removes it seconds later; retrying that would loop forever.
            if (wasStable) {
                lastConnectedDevice?.let { device ->
                    scope.launch {
                        delay(2_000L)
                        if (!isManualDisconnect && _connectionState.value == BleConnectionState.Disconnected) {
                            connect(device)
                        }
                    }
                }
            }
        }
    }

    init {
        midiManager?.registerDeviceCallback(midiDeviceCallback, mainHandler)
    }

    suspend fun emitIncomingCc(ccNumber: Int, value: Int, channel: Int = 0) {
        _incomingMidiCc.emit(MidiCcMessage(channel, ccNumber, value))
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name ?: result.scanRecord?.deviceName ?: "Unknown Device"
                val info = BleDeviceInfo(
                    device = device,
                    name = deviceName,
                    address = device.address,
                    rssi = result.rssi
                )
                val currentList = _scannedDevices.value.toMutableList()
                val index = currentList.indexOfFirst { it.address == device.address }
                if (index >= 0) {
                    currentList[index] = info
                } else {
                    currentList.add(info)
                }
                _scannedDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code $errorCode")
            _connectionState.value = BleConnectionState.Error("Scan failed: error code $errorCode")
        }
    }

    private fun parseAndEmitMidiBytes(bytes: ByteArray) {
        for (msg in BleMidiPacketParser.parse(bytes)) {
            scope.launch {
                _incomingMidiCc.emit(msg)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is disabled")
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Error("BLE Scanner not available")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(MIDI_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
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
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan", e)
        }
        if (_connectionState.value == BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        isManualDisconnect = false
        lastConnectedDevice = device
        connectionAttempt += 1
        val attempt = connectionAttempt

        closeMidiSession()
        publishConnectionState(BleConnectionState.Connecting(getBluetoothDeviceName(device)))

        val manager = midiManager
        if (manager == null) {
            _connectionState.value = BleConnectionState.Error("Android MIDI service unavailable")
            return
        }

        connectionTimeoutJob = scope.launch {
            delay(MIDI_OPEN_TIMEOUT_MS)
            if (attempt == connectionAttempt && _connectionState.value is BleConnectionState.Connecting) {
                Log.e(TAG, "Timed out opening MIDI device ${device.address}")
                closeMidiSession()
                publishConnectionState(BleConnectionState.Error(
                    "MIDI connection timed out. The WIDI may be busy in another app."
                ))
            }
        }

        // Song View may already have opened the Bluetooth MIDI device. Give Android's
        // MIDI service a moment to publish it, then open that shared device if present.
        scope.launch {
            delay(EXISTING_DEVICE_LOOKUP_DELAY_MS)
            if (attempt != connectionAttempt) return@launch

            try {
                val existingInfo = findExistingBluetoothMidiDevice(manager, device.address)
                // Android keeps a BLE MIDI device listed with a null connection status
                // after the WIDI is switched off (and openDevice() on it still succeeds
                // locally). Only reuse it when the Bluetooth link is really up, otherwise
                // both features would report a connection that does not exist.
                if (existingInfo != null && isBluetoothLinkConnected(device.address)) {
                    Log.d(TAG, "Reusing system MIDI device for ${device.address}")
                    manager.openDevice(existingInfo, createOpenListener(attempt, device), mainHandler)
                } else {
                    Log.d(TAG, "Opening Bluetooth MIDI device for ${device.address}")
                    manager.openBluetoothDevice(device, createOpenListener(attempt, device), mainHandler)
                }
            } catch (e: SecurityException) {
                failConnection(attempt, "Bluetooth permission missing", e)
            } catch (e: Exception) {
                failConnection(attempt, "Unable to open MIDI device: ${e.message}", e)
            }
        }
    }

    private fun findExistingBluetoothMidiDevice(
        manager: MidiManager,
        address: String
    ): MidiDeviceInfo? = manager.devices.firstOrNull { info ->
        if (info.type != MidiDeviceInfo.TYPE_BLUETOOTH) {
            false
        } else {
            @Suppress("DEPRECATION")
            val bluetoothDevice = info.properties.getParcelable(
                MidiDeviceInfo.PROPERTY_BLUETOOTH_DEVICE
            ) as? BluetoothDevice
            bluetoothDevice?.address.equals(address, ignoreCase = true)
        }
    }

    /**
     * True only when the tablet really has a Bluetooth link with [address].
     *
     * Android's MIDI service keeps a BLE MIDI device listed (with no connection
     * status) after the WIDI is powered off, and opening that descriptor succeeds
     * locally. Without this check the app reported "Connected" while nothing was
     * connected, and Song View and the Matrix Mixer could even disagree.
     */
    @SuppressLint("MissingPermission")
    private fun isBluetoothLinkConnected(address: String): Boolean {
        val manager = bluetoothManager ?: return false
        return try {
            manager.getConnectedDevices(BluetoothProfile.GATT).any { device ->
                device.address.equals(address, ignoreCase = true)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission while checking the Bluetooth link", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check the Bluetooth link", e)
            false
        }
    }

    private fun createOpenListener(
        attempt: Int,
        requestedDevice: BluetoothDevice
    ) = MidiManager.OnDeviceOpenedListener { openedDevice ->
        if (attempt != connectionAttempt) {
            openedDevice?.closeSafely()
            return@OnDeviceOpenedListener
        }

        if (openedDevice == null) {
            failConnection(attempt, "Android could not open the WIDI MIDI device")
            return@OnDeviceOpenedListener
        }

        try {
            val info = openedDevice.info
            if (info.inputPortCount == 0) {
                openedDevice.closeSafely()
                failConnection(attempt, "The MIDI device has no input port for sending controls")
                return@OnDeviceOpenedListener
            }

            val inputPort = openedDevice.openInputPort(0)
            if (inputPort == null) {
                openedDevice.closeSafely()
                failConnection(
                    attempt,
                    "WIDI is connected, but its MIDI input port is already in use by another app"
                )
                return@OnDeviceOpenedListener
            }

            midiDevice = openedDevice
            midiInputPort = inputPort
            connectedDeviceId = info.id

            if (info.outputPortCount > 0) {
                midiOutputPort = openedDevice.openOutputPort(0)?.also { outputPort ->
                    outputPort.connect(midiReceiver)
                }
            }

            connectionTimeoutJob?.cancel()
            val deviceName = getBluetoothDeviceName(requestedDevice)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            connectedAtMs = android.os.SystemClock.elapsedRealtime()
            publishConnectionState(BleConnectionState.Connected(deviceName, requestedDevice.address))
            Log.d(TAG, "Opened MIDI device ${info.id} for ${requestedDevice.address}")
        } catch (e: SecurityException) {
            openedDevice.closeSafely()
            failConnection(attempt, "Bluetooth permission missing", e)
        } catch (e: Exception) {
            openedDevice.closeSafely()
            failConnection(attempt, "Failed to open MIDI ports: ${e.message}", e)
        }
    }

    private fun failConnection(attempt: Int, message: String, error: Exception? = null) {
        if (attempt != connectionAttempt) return
        error?.let { Log.e(TAG, message, it) } ?: Log.e(TAG, message)
        connectionTimeoutJob?.cancel()
        closeMidiSession()
        publishConnectionState(BleConnectionState.Error(message))
    }

    @SuppressLint("MissingPermission")
    private fun getBluetoothDeviceName(device: BluetoothDevice): String? =
        try {
            device.name
        } catch (_: SecurityException) {
            null
        }

    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = BleConnectionState.Error("Bluetooth unavailable")
            return
        }
        try {
            connect(adapter.getRemoteDevice(address))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote device for address $address", e)
            _connectionState.value = BleConnectionState.Error("Invalid BLE device address")
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        connectionAttempt += 1
        closeMidiSession()
        publishConnectionState(BleConnectionState.Disconnected)
    }

    fun release() {
        isManualDisconnect = true
        connectionAttempt += 1
        stopScan()
        closeMidiSession()
        midiManager?.unregisterDeviceCallback(midiDeviceCallback)
        scope.cancel()
    }

    private fun closeMidiSession() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        connectedDeviceId = null
        connectedAtMs = 0L

        midiOutputPort?.let { port ->
            try {
                port.disconnect(midiReceiver)
                port.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing MIDI output port", e)
            }
        }
        midiOutputPort = null

        midiInputPort?.let { port ->
            try {
                port.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing MIDI input port", e)
            }
        }
        midiInputPort = null

        midiDevice?.closeSafely()
        midiDevice = null
    }

    private fun MidiDevice.closeSafely() {
        try {
            close()
        } catch (e: IOException) {
            Log.w(TAG, "Error closing MIDI device", e)
        }
    }

    @SuppressLint("MissingPermission")
    open fun sendControlChange(channel: Int, ccNumber: Int, value: Int): Boolean {
        val channel0 = if (channel in 1..16) channel - 1 else channel.coerceIn(0, 15)
        val message = byteArrayOf(
            (0xB0 or channel0).toByte(),
            (ccNumber and 0x7F).toByte(),
            (value and 0x7F).toByte()
        )
        return sendMidiMessage(message)
    }

    @SuppressLint("MissingPermission")
    fun sendMidiMessage(message: ByteArray): Boolean {
        val inputPort = midiInputPort ?: return false
        return try {
            inputPort.send(message, 0, message.size, System.nanoTime())
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error sending MIDI message", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to send MIDI message", e)
            false
        }
    }
}
