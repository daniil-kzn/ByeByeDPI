package io.github.romanvht.byedpi.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OobCharTest {

    @Test
    fun `oob char is passed as hex escape`() {
        assertEquals("\\x61", oobCharArg("a"))
        assertEquals("\\x20", oobCharArg(" "))
        assertEquals("\\x22", oobCharArg("\""))
        assertEquals("\\x5c", oobCharArg("\\"))
    }

    @Test
    fun `invalid oob char falls back to default`() {
        assertEquals("\\x61", oobCharArg(""))
        assertEquals("\\x61", oobCharArg("ab"))
        assertEquals("\\x61", oobCharArg("ж"))
    }

    @Test
    fun `oob char validation`() {
        assertTrue(isValidOobChar("a"))
        assertTrue(isValidOobChar("0"))
        assertFalse(isValidOobChar(""))
        assertFalse(isValidOobChar("ab"))
        assertFalse(isValidOobChar("ж"))
    }
}
