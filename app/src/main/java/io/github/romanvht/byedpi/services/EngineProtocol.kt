package io.github.romanvht.byedpi.services

import io.github.romanvht.byedpi.BuildConfig

internal object EngineProtocol {
    const val HELLO = 1
    const val START = 2
    const val STOP = 3
    const val HELLO_ACK = 4
    const val STARTED = 5
    const val FAILED = 6
    const val STOPPING = 7
    const val CONFIGURED = 8

    const val CONTROL_ACTION = "${BuildConfig.APPLICATION_ID}.ENGINE"
    const val BOUND_CONTROL_ACTION = "${BuildConfig.APPLICATION_ID}.BOUND_ENGINE"
    const val STATE_IDLE = 0
    const val STATE_STARTING = 1
    const val STATE_RUNNING = 2
    const val STATE_STOPPING = 3

    /** MTU of the TUN interface; hev-socks5-tunnel must use the same value. */
    const val TUN_MTU = 8500

    // Timeouts nest from the inside out, so the innermost failure, which carries
    // the real reason, always fires before an outer one cancels the whole start.
    /** Native process: SOCKS proxy must answer within this time. */
    const val NATIVE_START_TIMEOUT = 10_000L
    /** Client: the native process must answer HELLO within this time. */
    const val CONNECT_TIMEOUT = 10_000L
    /** Client: the engine must report STARTED within this time. */
    const val START_TIMEOUT = NATIVE_START_TIMEOUT + 5_000L
    /** Service action: the engine must accept the configuration within this time. */
    const val CONFIGURE_TIMEOUT = CONNECT_TIMEOUT + 2_000L

    const val ARGS = "args"
    const val HOST = "host"
    const val PORT = "port"
    const val DNS = "dns"
    const val IPV6 = "ipv6"
    const val APP_LIST_TYPE = "app_list_type"
    const val APPS = "apps"
    const val PID = "pid"
    const val STATE = "state"
    const val ERROR = "error"
}
