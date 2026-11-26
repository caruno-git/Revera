/* SPDX-License-Identifier: GPL-3.0-only */
package app.myspoti.spotify

import app.myspoti.spotify.bridge.AdSchedule
import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reported: "sometimes the music stops, I skip and it stays on the same song, paused". A skip the
 * page answers and never acts on is played by name, then the page is loaded again.
 */
class SkipWatchTest {
    private class FakeHost : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val calls = CopyOnWriteArrayList<String>()
        override suspend fun evaluate(script: String) {
            val m = Regex("""Myspoti\.call\((\d+), "([^"]+)", (.*)\)\.catch""", RegexOption.DOT_MATCHES_ALL).find(script) ?: return
            calls += m.groupValues[2]
            val value = if (m.groupValues[2] == "media.state") """{"paused":false,"muted":false,"level":1,"own":1,"time":0,"active":true}""" else "null"
            messages.emit("""{"type":"result","id":${m.groupValues[1]},"ok":true,"value":$value}""")
        }
    }

    private val time = VirtualTime()
    private lateinit var scope: CoroutineScope

    @BeforeEach fun up() { AdSchedule.clear(); scope = time.scope }
    @AfterEach fun down() { AdSchedule.clear(); scope.cancel() }

    private fun track(name: String) = """{"kind":"track","uri":"spotify:track:$name","uid":"u$name","name":"$name","durationMs":200000}"""

    private suspend fun stuckPlayer(host: FakeHost, next: Boolean = true): SpotifyPlayer {
        val player = SpotifyPlayer(Bridge(host, scope, time::now), scope)
        host.messages.emit("""{"type":"ready"}""")
        val after = if (next) """"next":${track("B")},""" else ""
        host.messages.emit("""{"type":"state","state":{"track":${track("A")},"contextUri":"spotify:playlist:C",$after"isPaused":true,"isBuffering":false,
            |"repeat":0,"positionMs":0,"timestamp":${time.now()},"restrictions":{}}}""".trimMargin())
        time.waitFor("the page's device was read", 8_000) { "media.state" in host.calls }
        return player
    }

    @Test fun `a skip that never moves the page loads the page again`() = time.test {
        val host = FakeHost()
        val player = stuckPlayer(host)
        val reloads = AtomicInteger()
        player.reloadHost = { reloads.incrementAndGet() }
        player.next()
        delay(SpotifyPlayer.SKIP_WATCH_MS + SpotifyPlayer.SKIP_REPLAY_MS + 2_000)
        assertTrue("player.play" in host.calls, host.calls.toString())
        assertEquals(1, reloads.get())
    }

    @Test fun `a skip with no song after it is left alone`() = time.test {
        val host = FakeHost()
        val player = stuckPlayer(host, next = false)
        val reloads = AtomicInteger()
        player.reloadHost = { reloads.incrementAndGet() }
        player.next()
        delay(SpotifyPlayer.SKIP_WATCH_MS + SpotifyPlayer.SKIP_REPLAY_MS + 2_000)
        assertEquals(0, reloads.get())
    }

    @Test fun `a pause after the skip ends the watch`() = time.test {
        val host = FakeHost()
        val player = stuckPlayer(host)
        val reloads = AtomicInteger()
        player.reloadHost = { reloads.incrementAndGet() }
        player.next()
        player.pause()
        delay(SpotifyPlayer.SKIP_WATCH_MS + SpotifyPlayer.SKIP_REPLAY_MS + 2_000)
        assertEquals(0, reloads.get())
    }
}
