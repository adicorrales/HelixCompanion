package adco.helixmidimixer.ui.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adco.helixmidimixer.data.model.ChannelState
import adco.helixmidimixer.data.model.HomeRoute
import adco.helixmidimixer.data.model.MidiCcMapping
import adco.helixmidimixer.data.model.OutputGroup
import adco.helixmidimixer.data.model.SongPlayMode
import adco.helixmidimixer.data.repository.DataStoreRepository
import adco.helixmidimixer.data.repository.MixerWindowSettings
import adco.helixcompanion.midi.BleConnectionState
import adco.helixcompanion.midi.BleDeviceInfo
import adco.helixcompanion.midi.BleMidiManager
import adco.helixcompanion.midi.MidiSender
import adco.helixcompanion.midi.TransportManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DataStoreRepository,
    val bleMidiManager: BleMidiManager
) : ViewModel() {

    private val _selectedGroup = MutableStateFlow(OutputGroup.OUTPUT_1_4)
    val selectedGroup: StateFlow<OutputGroup> = _selectedGroup.asStateFlow()

    // Local in-memory state for volume (0..127) per group and channel (key: "GROUP_chIndex")
    private val _volumes = MutableStateFlow<Map<String, Int>>(emptyMap())
    // Local in-memory state for pan (0..127) per group and channel (key: "GROUP_chIndex")
    private val _pans = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val ccJobs = mutableMapOf<String, Job>()
    private val dbSaveJobs = mutableMapOf<String, Job>()
    private val lastCcSendTimes = mutableMapOf<String, Long>()

    val mixerWindowSettings: StateFlow<MixerWindowSettings> = repository.getMixerWindowSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MixerWindowSettings())

    val midiCcMapping: StateFlow<MidiCcMapping> = repository.getMidiCcMapping()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MidiCcMapping())

    val matrixMidiChannels: StateFlow<Map<OutputGroup, Int>> = repository.getMatrixMidiChannels()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            mapOf(
                OutputGroup.OUTPUT_1_4 to 3,
                OutputGroup.XLR to 4,
                OutputGroup.PHONES to 5
            )
        )

    // Transport configuration: one source (TransportManager) shared with Song View.
    val transportMidiChannel: StateFlow<Int> = TransportManager.transportChannel

    val autoReconnectEnabled: StateFlow<Boolean> = repository.getAutoReconnectEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val songPlayMode: StateFlow<SongPlayMode> = TransportManager.playMode
        .map { name -> if (name.contains("Load", ignoreCase = true)) SongPlayMode.LOAD else SongPlayMode.PLAY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SongPlayMode.LOAD)

    // Transport state: owned by TransportManager, observed here.
    val isPlaying: StateFlow<Boolean> = TransportManager.isPlaying
    val playStatus: StateFlow<String> = TransportManager.playStatus
    val currentSongTitle: StateFlow<String> = TransportManager.currentTitle
    val currentSongKey: StateFlow<String> = TransportManager.currentKey
    val currentSongCapo: StateFlow<String> = TransportManager.currentCapo
    val currentSongTempo: StateFlow<String> = TransportManager.currentTempo
    val prevSongTitle: StateFlow<String> = TransportManager.prevTitle
    val nextSongTitle: StateFlow<String> = TransportManager.nextTitle

    // Echo suppression is keyed by channel + CC + value: the Helix answers our
    // CC51:127 with CC51:1 (start-from-zero) and that notification must not be
    // mistaken for an echo of the value we sent.
    private val lastSentCcTime = mutableMapOf<String, Long>()

    private fun ccEchoKey(channel: Int, ccNumber: Int, value: Int) = "$channel:$ccNumber:$value"

    private fun markCcSent(channel: Int, ccNumber: Int, value: Int) {
        lastSentCcTime[ccEchoKey(channel, ccNumber, value)] = System.currentTimeMillis()
    }

    private fun isCcSelfEcho(channel: Int, ccNumber: Int, value: Int): Boolean {
        val sentTime = lastSentCcTime[ccEchoKey(channel, ccNumber, value)] ?: return false
        return (System.currentTimeMillis() - sentTime) < 400L
    }

    val routesExpanded: StateFlow<Map<OutputGroup, Boolean>> = repository.getRoutesExpanded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val songChannelsExpanded: StateFlow<Map<OutputGroup, Boolean>> = repository.getSongChannelsExpanded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val channelVisibilities: StateFlow<Map<String, Boolean>> = repository.getChannelVisibilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val bleConnectionState: StateFlow<BleConnectionState> = bleMidiManager.connectionState
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = bleMidiManager.scannedDevices

    private val songChannelNames = repository.getSongChannelNames()
    private val muteStates = repository.getMuteStates()
    private val soloStates = repository.getSoloStates()
    private val homeRoutes = repository.getHomeRoutes()

    val channelStates: StateFlow<List<ChannelState>> = combine(
        _selectedGroup,
        _volumes,
        _pans,
        songChannelNames,
        muteStates,
        soloStates,
        homeRoutes,
        routesExpanded,
        channelVisibilities,
        songChannelsExpanded
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val group = flows[0] as OutputGroup
        @Suppress("UNCHECKED_CAST")
        val volumes = flows[1] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val pans = flows[2] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val songNames = flows[3] as Map<Int, String>
        @Suppress("UNCHECKED_CAST")
        val mutes = flows[4] as Map<String, Boolean>
        @Suppress("UNCHECKED_CAST")
        val solos = flows[5] as Map<String, Boolean>
        @Suppress("UNCHECKED_CAST")
        val routes = flows[6] as Map<OutputGroup, HomeRoute>
        @Suppress("UNCHECKED_CAST")
        val expandedMap = flows[7] as Map<OutputGroup, Boolean>
        @Suppress("UNCHECKED_CAST")
        val visibilities = flows[8] as Map<String, Boolean>
        @Suppress("UNCHECKED_CAST")
        val songExpandedMap = flows[9] as Map<OutputGroup, Boolean>

        val isExpanded = expandedMap[group] ?: false
        val isSongExpanded = songExpandedMap[group] ?: true

        (0..18).mapNotNull { chIndex ->
            val key = "${group.name}_$chIndex"
            val isVisible = visibilities[key] ?: true
            if (!isVisible) {
                return@mapNotNull null
            }

            // If routes are collapsed, hide route channels (indices 1, 2, 3, 4: Path 1A, 1B, 2A, 2B)
            if (!isExpanded && chIndex in 1..4) {
                return@mapNotNull null
            }

            // If song channels are collapsed, hide song channels (indices 6..13: CH 1..8)
            if (!isSongExpanded && chIndex in 6..13) {
                return@mapNotNull null
            }

            val customName = if (chIndex in 6..13) {
                val songIdx = chIndex - 5 // 1..8
                songNames[songIdx]
            } else null

            val volumeVal = volumes[key] ?: 108 // Default 108 CC (0dB)
            val panVal = pans[key] ?: 64        // Default Center (64)
            val isMutedVal = mutes[key] ?: false
            val isSoloedVal = solos[key] ?: false
            val routeVal = routes[group] ?: HomeRoute.ROUTE_1A

            ChannelState(
                group = group,
                channelIndex = chIndex,
                customName = customName,
                volume = volumeVal,
                pan = panVal,
                isMuted = isMutedVal,
                isSoloed = isSoloedVal,
                homeRoute = routeVal
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect selected group from DataStore repository
        viewModelScope.launch {
            repository.getSelectedGroup().collect { group ->
                _selectedGroup.value = group
            }
        }
        // Collect volume & pan states from DataStore repository
        viewModelScope.launch {
            repository.getVolumeStates().collect { savedVols ->
                val current = _volumes.value.toMutableMap()
                savedVols.forEach { (k, v) ->
                    if (!current.containsKey(k)) {
                        current[k] = v
                    }
                }
                _volumes.value = current
            }
        }
        viewModelScope.launch {
            repository.getPanStates().collect { savedPans ->
                val current = _pans.value.toMutableMap()
                savedPans.forEach { (k, v) ->
                    if (!current.containsKey(k)) {
                        current[k] = v
                    }
                }
                _pans.value = current
            }
        }
        // Listen to incoming MIDI CC packets over BLE
        viewModelScope.launch {
            bleMidiManager.incomingMidiCc.collect { ccMsg ->
                handleIncomingMidiCc(ccMsg.ccNumber, ccMsg.value, ccMsg.channel)
            }
        }

        // Auto-reconnect to last used device on startup if enabled
        viewModelScope.launch {
            try {
                val enabled = repository.getAutoReconnectEnabled().first()
                if (enabled) {
                    val address = repository.getLastBleDeviceAddress().first()
                    if (!address.isNullOrBlank()) {
                        bleMidiManager.connectByAddress(address)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getActiveMatrixMidiChannel(group: OutputGroup = _selectedGroup.value): Int {
        val defaultCh = when (group) {
            OutputGroup.OUTPUT_1_4 -> 3
            OutputGroup.XLR -> 4
            OutputGroup.PHONES -> 5
        }
        return matrixMidiChannels.value[group] ?: defaultCh
    }

    fun selectOutputGroup(group: OutputGroup) {
        _selectedGroup.value = group
        viewModelScope.launch {
            repository.saveSelectedGroup(group)
        }
    }

    fun onVolumeChange(channelIndex: Int, volume: Int) {
        updateVolume(channelIndex, volume)
    }

    fun updateVolume(channelIndex: Int, volume: Int) {
        val group = _selectedGroup.value
        val key = "${group.name}_$channelIndex"
        val clampedVol = volume.coerceIn(0, 127)
        val currentVols = _volumes.value
        if (currentVols[key] != clampedVol) {
            val mutableVols = currentVols.toMutableMap()
            mutableVols[key] = clampedVol
            _volumes.value = mutableVols

            // Debounced DataStore save (save 300ms after dragging stops)
            val dbKey = "vol_$key"
            dbSaveJobs[dbKey]?.cancel()
            dbSaveJobs[dbKey] = viewModelScope.launch {
                delay(300L)
                repository.saveVolumeState(group, channelIndex, clampedVol)
            }

            // Throttled BLE MIDI CC Send (send max 1 CC per 35ms + guaranteed final send when dragging stops)
            val channelCcMapping = midiCcMapping.value.getChannelCc(group, channelIndex)
            val midiCh = getActiveMatrixMidiChannel(group)
            val ccNumber = channelCcMapping.volumeCc
            val ccKey = "vol_cc_$key"

            val now = System.currentTimeMillis()
            val lastSend = lastCcSendTimes[ccKey] ?: 0L

            ccJobs[ccKey]?.cancel()

            if (now - lastSend >= 35L) {
                lastCcSendTimes[ccKey] = now
                markCcSent(midiCh, ccNumber, clampedVol)
                MidiSender.sendCc(channel = midiCh, ccNumber = ccNumber, value = clampedVol)
            } else {
                ccJobs[ccKey] = viewModelScope.launch {
                    delay(40L)
                    lastCcSendTimes[ccKey] = System.currentTimeMillis()
                    markCcSent(midiCh, ccNumber, clampedVol)
                    MidiSender.sendCc(channel = midiCh, ccNumber = ccNumber, value = clampedVol)
                }
            }
        }
    }

    fun onPanChange(channelIndex: Int, pan: Int) {
        updatePan(channelIndex, pan)
    }

    fun updatePan(channelIndex: Int, pan: Int) {
        val group = _selectedGroup.value
        val key = "${group.name}_$channelIndex"
        val clampedPan = pan.coerceIn(0, 127)
        val currentPans = _pans.value
        if (currentPans[key] != clampedPan) {
            val mutablePans = currentPans.toMutableMap()
            mutablePans[key] = clampedPan
            _pans.value = mutablePans

            // Debounced DataStore save
            val dbKey = "pan_$key"
            dbSaveJobs[dbKey]?.cancel()
            dbSaveJobs[dbKey] = viewModelScope.launch {
                delay(300L)
                repository.savePanState(group, channelIndex, clampedPan)
            }

            // Throttled BLE MIDI CC Send
            val channelCcMapping = midiCcMapping.value.getChannelCc(group, channelIndex)
            val midiCh = getActiveMatrixMidiChannel(group)
            val ccNumber = channelCcMapping.panCc
            val ccKey = "pan_cc_$key"

            val now = System.currentTimeMillis()
            val lastSend = lastCcSendTimes[ccKey] ?: 0L

            ccJobs[ccKey]?.cancel()

            if (now - lastSend >= 35L) {
                lastCcSendTimes[ccKey] = now
                markCcSent(midiCh, ccNumber, clampedPan)
                MidiSender.sendCc(channel = midiCh, ccNumber = ccNumber, value = clampedPan)
            } else {
                ccJobs[ccKey] = viewModelScope.launch {
                    delay(40L)
                    lastCcSendTimes[ccKey] = System.currentTimeMillis()
                    markCcSent(midiCh, ccNumber, clampedPan)
                    MidiSender.sendCc(channel = midiCh, ccNumber = ccNumber, value = clampedPan)
                }
            }
        }
    }

    fun toggleMute(channelIndex: Int, isMuted: Boolean? = null) {
        val group = _selectedGroup.value
        val targetMuteState = isMuted ?: run {
            val currentChannelState = channelStates.value.find { it.channelIndex == channelIndex }
            !(currentChannelState?.isMuted ?: false)
        }
        viewModelScope.launch {
            repository.saveMuteState(group, channelIndex, targetMuteState)
        }
        // Send BLE MIDI CC (value 127 >= 64 to toggle state per Helix specification)
        val channelCcMapping = midiCcMapping.value.getChannelCc(group, channelIndex)
        val midiCh = getActiveMatrixMidiChannel(group)
        markCcSent(midiCh, channelCcMapping.muteCc, 127)
        MidiSender.sendCc(
            channel = midiCh,
            ccNumber = channelCcMapping.muteCc,
            value = 127
        )
    }

    fun toggleSolo(channelIndex: Int, isSoloed: Boolean? = null) {
        val group = _selectedGroup.value
        val targetSoloState = isSoloed ?: run {
            val currentChannelState = channelStates.value.find { it.channelIndex == channelIndex }
            !(currentChannelState?.isSoloed ?: false)
        }
        viewModelScope.launch {
            repository.saveSoloState(group, channelIndex, targetSoloState)
        }
        // Send BLE MIDI CC (value 127 >= 64 to toggle state per Helix specification)
        val channelCcMapping = midiCcMapping.value.getChannelCc(group, channelIndex)
        val midiCh = getActiveMatrixMidiChannel(group)
        markCcSent(midiCh, channelCcMapping.soloCc, 127)
        MidiSender.sendCc(
            channel = midiCh,
            ccNumber = channelCcMapping.soloCc,
            value = 127
        )
    }

    fun toggleChannelVisibility(channelIndex: Int, isVisible: Boolean) {
        val group = _selectedGroup.value
        viewModelScope.launch {
            repository.saveChannelVisibility(group, channelIndex, isVisible)
        }
    }

    fun clearAllSolos() {
        val group = _selectedGroup.value
        viewModelScope.launch {
            for (chIdx in 0..18) {
                repository.saveSoloState(group, chIdx, false)
            }
        }
        // Send Clear Solo CC (CC 94, value 127 >= 64)
        val midiCh = getActiveMatrixMidiChannel(group)
        markCcSent(midiCh, 94, 127)
        MidiSender.sendCc(channel = midiCh, ccNumber = 94, value = 127)
    }

    fun updateHomeRoute(route: HomeRoute) {
        val group = _selectedGroup.value
        viewModelScope.launch {
            repository.saveHomeRoute(group, route)
        }
    }

    fun saveSongChannelName(songIndex: Int, newName: String) {
        viewModelScope.launch {
            repository.saveSongChannelName(songIndex, newName)
        }
    }

    fun updateMidiCcMapping(mapping: MidiCcMapping) {
        viewModelScope.launch {
            repository.saveMidiCcMapping(mapping)
        }
    }

    fun saveMatrixMidiChannel(group: OutputGroup, channel: Int) {
        viewModelScope.launch {
            repository.saveMatrixMidiChannel(group, channel)
        }
    }

    fun sendTransportPrev() = TransportManager.prev()

    fun sendTransportReturnToZero() = TransportManager.rt0()

    fun toggleTransportPlay() = TransportManager.play()

    fun sendTransportNext() = TransportManager.next()

    fun toggleRoutesExpanded(group: OutputGroup, expanded: Boolean) {
        viewModelScope.launch {
            repository.saveRoutesExpanded(group, expanded)
        }
    }

    fun toggleSongChannelsExpanded(group: OutputGroup, expanded: Boolean) {
        viewModelScope.launch {
            repository.saveSongChannelsExpanded(group, expanded)
        }
    }

    fun startBleScan() {
        bleMidiManager.startScan()
    }

    fun stopBleScan() {
        bleMidiManager.stopScan()
    }

    fun connectBleDevice(device: BluetoothDevice) {
        bleMidiManager.connect(device)
        viewModelScope.launch {
            repository.saveLastBleDeviceAddress(device.address)
        }
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoReconnectEnabled(enabled)
        }
    }

    fun disconnectBleDevice() {
        bleMidiManager.disconnect()
    }

    override fun onCleared() {
        bleMidiManager.stopScan()
        super.onCleared()
    }

    private fun handleIncomingMidiCc(ccNumber: Int, value: Int, channel: Int) {
        val mapping = midiCcMapping.value

        // Ignore only exact self-echoes from app-initiated CC sends (same channel,
        // CC and value). The Helix reports a hardware start with CC51:1 right after
        // our CC51:127, and that response must reach the transport state.
        if (isCcSelfEcho(channel, ccNumber, value)) {
            return
        }

        // Transport feedback (CC51 / 49 / 47 / 10 / 63) is owned by
        // TransportManager, the single handler for both delivery paths (this
        // parsed CC flow and the forwarded MidiReceiver used by Song View).
        if (TransportManager.onFeedback(channel, ccNumber, value)) {
            return
        }

        // Matrix feedback is per output group, and each group owns a MIDI channel
        // (3/4/5 by default). Route the message by channel instead of applying it
        // to whichever group is selected; fall back to the selected group when the
        // device answers on an unexpected channel.
        val group = OutputGroup.entries.firstOrNull { candidate ->
            getActiveMatrixMidiChannel(candidate) - 1 == channel
        } ?: _selectedGroup.value

        // Handle Clear Solo CC (CC 94)
        if (ccNumber == 94 && value >= 64) {
            viewModelScope.launch {
                for (chIdx in 0..18) {
                    repository.saveSoloState(group, chIdx, false)
                }
            }
            return
        }

        for (chIndex in 0..18) {
            val chCc = mapping.getChannelCc(group, chIndex)
            val key = "${group.name}_$chIndex"
            if (ccNumber == chCc.volumeCc) {
                val clampedValue = value.coerceIn(0, 127)
                val vols = _volumes.value
                if (vols[key] != clampedValue) {
                    val mutableVols = vols.toMutableMap()
                    mutableVols[key] = clampedValue
                    _volumes.value = mutableVols
                }
            }
            if (ccNumber == chCc.panCc) {
                val clampedValue = value.coerceIn(0, 127)
                val pns = _pans.value
                if (pns[key] != clampedValue) {
                    val mutablePns = pns.toMutableMap()
                    mutablePns[key] = clampedValue
                    _pans.value = mutablePns
                }
            }
            if (ccNumber == chCc.muteCc) {
                val isMuted = value >= 64
                viewModelScope.launch {
                    repository.saveMuteState(group, chIndex, isMuted)
                }
            }
            if (ccNumber == chCc.soloCc) {
                val isSoloed = value >= 64
                viewModelScope.launch {
                    repository.saveSoloState(group, chIndex, isSoloed)
                }
            }
        }
    }
}
