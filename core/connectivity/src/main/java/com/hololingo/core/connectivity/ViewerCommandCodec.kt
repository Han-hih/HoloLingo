package com.hololingo.core.connectivity

import java.nio.charset.StandardCharsets

object ViewerCommandCodec {
    private const val VERSION = "1"
    private const val PLAY_SCENE = "PLAY_SCENE"

    fun encode(command: ViewerCommand): ByteArray = when (command) {
        is ViewerCommand.PlayScene -> listOf(
            VERSION,
            PLAY_SCENE,
            command.sceneId.safeField(),
            command.conceptId.safeField(),
        ).joinToString("|").toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray): ViewerCommand? {
        val fields = bytes.toString(StandardCharsets.UTF_8).split('|')
        if (fields.size != 4 || fields[0] != VERSION) return null
        return when (fields[1]) {
            PLAY_SCENE -> ViewerCommand.PlayScene(fields[2], fields[3])
            else -> null
        }
    }

    private fun String.safeField(): String = replace("|", "_")
}
