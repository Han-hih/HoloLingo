package com.hololingo.core.connectivity

data class NearbyDevice(
    val endpointId: String,
    val name: String,
)

enum class DeviceRole {
    LEARNING_TABLET,
    HOLOGRAM_VIEWER,
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data class Advertising(val deviceName: String) : ConnectionState
    data class Discovering(val devices: List<NearbyDevice>) : ConnectionState
    data class Pairing(
        val deviceName: String,
        val authenticationDigits: String,
        val role: DeviceRole,
        val isLocallyAccepted: Boolean = false,
    ) : ConnectionState
    data class Connected(
        val deviceName: String,
        val role: DeviceRole,
    ) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

sealed interface ViewerCommand {
    data class PlayScene(
        val sceneId: String,
        val conceptId: String,
    ) : ViewerCommand
}
