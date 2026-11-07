package io.github.romanvht.byedpi.utility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalStrategyTest {

    @Test
    fun `strategy without listen address is allowed`() {
        assertTrue(strategyListensOnLoopback("-o1 -a1 -At,r,s,c -f-1 -r1+s -a1"))
    }

    @Test
    fun `loopback listen addresses are allowed`() {
        assertTrue(strategyListensOnLoopback("-i127.0.0.1 -o1"))
        assertTrue(strategyListensOnLoopback("--ip 127.0.0.2 -o1"))
        assertTrue(strategyListensOnLoopback("--ip=::1 -o1"))
        assertTrue(strategyListensOnLoopback("-i [::1] -o1"))
        assertTrue(strategyListensOnLoopback("--ip localhost -o1"))
    }

    @Test
    fun `wildcard and network addresses are rejected`() {
        assertFalse(strategyListensOnLoopback("-i0.0.0.0 -o1"))
        assertFalse(strategyListensOnLoopback("--ip :: -o1"))
        assertFalse(strategyListensOnLoopback("--ip=192.168.1.10 -o1"))
        assertFalse(strategyListensOnLoopback("-o1 -i 10.0.0.1"))
        assertFalse(strategyListensOnLoopback("--ip example.com -o1"))
    }

    @Test
    fun `listen address hidden in combined short flags is found`() {
        assertFalse(strategyListensOnLoopback("-Ni0.0.0.0 -o1"))
    }
}
