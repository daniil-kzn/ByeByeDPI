package io.github.romanvht.byedpi.services

import android.app.Service

/**
 * Isolated engine slots for the strategy test.
 *
 * byedpi keeps its state in globals and cannot be restarted inside one process, so
 * every running engine needs a process of its own. Each slot is a bound-only copy of
 * [ByeDpiProxyService] declared in the manifest with `android:process=":testN"`,
 * which lets up to [COUNT] strategies run at the same time without touching the
 * user's `:native` process.
 */
internal object TestSlots {
    const val COUNT = 4

    private val services: List<Class<out Service>> = listOf(
        ProxyTestSlot0::class.java,
        ProxyTestSlot1::class.java,
        ProxyTestSlot2::class.java,
        ProxyTestSlot3::class.java,
    )

    init {
        check(services.size == COUNT)
    }

    fun serviceClass(slot: Int): Class<out Service> = services[slot]

    fun processSuffix(slot: Int): String = ":test$slot"
}

class ProxyTestSlot0 : ByeDpiProxyService()
class ProxyTestSlot1 : ByeDpiProxyService()
class ProxyTestSlot2 : ByeDpiProxyService()
class ProxyTestSlot3 : ByeDpiProxyService()
