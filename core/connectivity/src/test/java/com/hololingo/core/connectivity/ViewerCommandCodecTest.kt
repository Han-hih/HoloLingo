package com.hololingo.core.connectivity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewerCommandCodecTest {
    @Test
    fun `재생 명령을 인코딩하고 복원한다`() {
        val command = ViewerCommand.PlayScene("mountain", "MOUNTAIN")

        assertEquals(command, ViewerCommandCodec.decode(ViewerCommandCodec.encode(command)))
    }

    @Test
    fun `지원하지 않는 메시지는 무시한다`() {
        assertNull(ViewerCommandCodec.decode("2|PLAY_SCENE|mountain|MOUNTAIN".toByteArray()))
    }
}
