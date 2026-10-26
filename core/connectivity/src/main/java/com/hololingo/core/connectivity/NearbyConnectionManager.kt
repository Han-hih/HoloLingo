package com.hololingo.core.connectivity

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyConnectionManager(context: Context) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val _commands = MutableSharedFlow<ViewerCommand>(extraBufferCapacity = 8)
    private val devices = linkedMapOf<String, NearbyDevice>()

    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    val commands: SharedFlow<ViewerCommand> = _commands.asSharedFlow()

    private var localName = "HoloLingo"
    private var role = DeviceRole.LEARNING_TABLET
    private var pendingEndpointId: String? = null
    private var connectedEndpointId: String? = null
    private var remoteName = "상대 기기"

    fun startViewer(deviceName: String) {
        resetEndpoints()
        localName = deviceName
        role = DeviceRole.HOLOGRAM_VIEWER
        client.startAdvertising(
            deviceName,
            SERVICE_ID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnSuccessListener {
            _state.value = ConnectionState.Advertising(deviceName)
        }.addOnFailureListener { error ->
            _state.value = ConnectionState.Error(error.localizedMessage ?: "기기 광고를 시작하지 못했습니다.")
        }
    }

    fun startController(deviceName: String) {
        resetEndpoints()
        localName = deviceName
        role = DeviceRole.LEARNING_TABLET
        devices.clear()
        _state.value = ConnectionState.Discovering(emptyList())
        client.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { error ->
            _state.value = ConnectionState.Error(error.localizedMessage ?: "주변 기기를 찾지 못했습니다.")
        }
    }

    fun requestConnection(device: NearbyDevice) {
        remoteName = device.name
        client.stopDiscovery()
        client.requestConnection(localName, device.endpointId, connectionLifecycleCallback)
            .addOnFailureListener { error ->
                _state.value = ConnectionState.Error(error.localizedMessage ?: "연결을 요청하지 못했습니다.")
            }
    }

    fun acceptPairing() {
        val endpointId = pendingEndpointId ?: return
        val pairing = _state.value as? ConnectionState.Pairing ?: return
        if (pairing.isLocallyAccepted) return
        _state.value = pairing.copy(isLocallyAccepted = true)
        client.acceptConnection(endpointId, payloadCallback).addOnFailureListener { error ->
            _state.value = ConnectionState.Error(error.localizedMessage ?: "연결 확인에 실패했습니다.")
        }
    }

    fun rejectPairing() {
        pendingEndpointId?.let(client::rejectConnection)
        pendingEndpointId = null
        _state.value = ConnectionState.Error("연결을 취소했습니다.")
    }

    fun send(command: ViewerCommand): Boolean {
        val endpointId = connectedEndpointId ?: return false
        client.sendPayload(endpointId, Payload.fromBytes(ViewerCommandCodec.encode(command)))
            .addOnFailureListener { error ->
                _state.value = ConnectionState.Error(error.localizedMessage ?: "재생 명령 전송에 실패했습니다.")
            }
        return true
    }

    fun disconnect() {
        resetEndpoints()
        _state.value = ConnectionState.Idle
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            devices[endpointId] = NearbyDevice(endpointId, info.endpointName)
            _state.value = ConnectionState.Discovering(devices.values.toList())
        }

        override fun onEndpointLost(endpointId: String) {
            devices.remove(endpointId)
            _state.value = ConnectionState.Discovering(devices.values.toList())
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            pendingEndpointId = endpointId
            remoteName = info.endpointName
            _state.value = ConnectionState.Pairing(
                deviceName = info.endpointName,
                authenticationDigits = info.authenticationDigits,
                role = role,
            )
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            pendingEndpointId = null
            if (resolution.status.isSuccess) {
                connectedEndpointId = endpointId
                client.stopAdvertising()
                client.stopDiscovery()
                _state.value = ConnectionState.Connected(remoteName, role)
            } else {
                _state.value = ConnectionState.Error("연결을 완료하지 못했습니다. 다시 시도해 주세요.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (endpointId == connectedEndpointId) connectedEndpointId = null
            _state.value = ConnectionState.Error("상대 기기와의 연결이 끊어졌습니다.")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let(ViewerCommandCodec::decode)?.let(_commands::tryEmit)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private fun resetEndpoints() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        pendingEndpointId = null
        connectedEndpointId = null
        devices.clear()
    }

    companion object {
        private const val SERVICE_ID = "com.hololingo.connection"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}
