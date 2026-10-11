package io.nekohasekai.sagernet.fmt.shadowsocks

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration check that the shared-core Shadowsocks parser is mapped onto [ShadowsocksBean].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ShadowsocksFmtTest {

    // base64("aes-256-gcm:pass")
    private val b64 = "YWVzLTI1Ni1nY206cGFzcw=="

    // base64("aes-256-gcm:pass@1.2.3.4:8388")
    private val b64Whole = "YWVzLTI1Ni1nY206cGFzc0AxLjIuMy40OjgzODg="

    @Test
    fun parsesAndroidStyle() {
        val bean = parseShadowsocks("ss://$b64@1.2.3.4:8388#node")
        assertEquals("1.2.3.4", bean.serverAddress)
        assertEquals(8388, bean.serverPort)
        assertEquals("aes-256-gcm", bean.method)
        assertEquals("pass", bean.password)
        assertEquals("node", bean.name)
    }

    @Test
    fun parsesV2RayNStyle() {
        val bean = parseShadowsocks("ss://$b64Whole#my%20node")
        assertEquals("1.2.3.4", bean.serverAddress)
        assertEquals(8388, bean.serverPort)
        assertEquals("aes-256-gcm", bean.method)
        assertEquals("pass", bean.password)
        assertEquals("my node", bean.name)
    }

    @Test
    fun renamesSimpleObfsPlugin() {
        val bean = parseShadowsocks("ss://$b64@1.2.3.4:8388?plugin=simple-obfs%3Bobfs%3Dhttp")
        assertEquals("obfs-local;obfs=http", bean.plugin)
    }
}
