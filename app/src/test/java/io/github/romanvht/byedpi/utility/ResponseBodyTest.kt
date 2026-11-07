package io.github.romanvht.byedpi.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException

class ResponseBodyTest {

    /** Returns [bytes] bytes, then fails the way a frozen connection does. */
    private class FreezingStream(private var bytes: Int) : InputStream() {
        override fun read(): Int = throw UnsupportedOperationException()
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (bytes <= 0) throw SocketTimeoutException("Read timed out")
            val n = minOf(len, bytes)
            bytes -= n
            return n
        }
    }

    private fun stream(size: Int) = ByteArrayInputStream(ByteArray(size))

    @Test
    fun `chunked body cut by timeout is a failure`() {
        val result = readResponseBody(FreezingStream(16 * 1024), declaredLength = -1)
        assertFalse(result.isComplete)
        assertEquals(16 * 1024L, result.length)
    }

    @Test
    fun `chunked body that ends normally is a success`() {
        val result = readResponseBody(stream(5_000), declaredLength = -1)
        assertTrue(result.isComplete)
        assertEquals(5_000L, result.length)
    }

    @Test
    fun `body with declared length read in full is a success`() {
        assertTrue(readResponseBody(stream(10_000), declaredLength = 10_000).isComplete)
    }

    @Test
    fun `body shorter than declared length is a failure`() {
        assertFalse(readResponseBody(stream(4_000), declaredLength = 10_000).isComplete)
    }

    @Test
    fun `declared body cut by timeout is a failure`() {
        assertFalse(readResponseBody(FreezingStream(20_000), declaredLength = 1_000_000).isComplete)
    }

    @Test
    fun `reading stops at the limit and counts as success`() {
        val result = readResponseBody(stream(1_000_000), declaredLength = -1, limit = 64 * 1024)
        assertTrue(result.isComplete)
        assertEquals(64 * 1024L, result.length)
    }

    @Test
    fun `large declared body passes after the limit without full download`() {
        val result = readResponseBody(stream(1_000_000), declaredLength = 1_000_000, limit = 64 * 1024)
        assertTrue(result.isComplete)
        assertEquals(64 * 1024L, result.length)
    }

    @Test
    fun `missing body is a success`() {
        assertTrue(readResponseBody(null, declaredLength = -1).isComplete)
    }

    @Test
    fun `cancellation check is called while reading`() {
        var calls = 0
        readResponseBody(stream(40_000), declaredLength = -1) { calls++ }
        assertTrue(calls > 1)
    }

    @Test(expected = IllegalStateException::class)
    fun `cancellation propagates`() {
        readResponseBody(stream(40_000), declaredLength = -1) { throw IllegalStateException() }
    }

    @Test
    fun `io error on first read is a failure`() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("reset")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("reset")
        }
        assertFalse(readResponseBody(broken, declaredLength = -1).isComplete)
    }
}
