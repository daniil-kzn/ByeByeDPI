package io.github.romanvht.byedpi.utility

import android.content.Context
import androidx.core.content.edit
import java.net.InetAddress
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Access rules for [io.github.romanvht.byedpi.activities.ToggleActivity], which is
 * exported so that automation apps (Tasker, `am start`) can drive the service.
 *
 * Launcher shortcuts created by the app carry a random token and are always trusted.
 * Any other caller is untrusted: it may start or stop the service only while
 * [PREF_ALLOW_CONTROL] is on, and may replace the strategy only while
 * [PREF_ALLOW_STRATEGY] is on and the strategy keeps the proxy on a loopback address.
 */
object ExternalControl {
    const val EXTRA_TOKEN = "token"
    const val PREF_ALLOW_CONTROL = "external_control"
    const val PREF_ALLOW_STRATEGY = "external_strategy"
    const val TOKEN_KEY = "shortcut_token"

    fun token(context: Context): String {
        val prefs = context.getPreferences()
        prefs.getString(TOKEN_KEY, null)?.takeIf { it.isNotEmpty() }?.let { return it }
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit(commit = true) { putString(TOKEN_KEY, token) }
        return token
    }

    fun isTrusted(context: Context, token: String?): Boolean {
        if (token.isNullOrEmpty()) return false
        return MessageDigest.isEqual(token.toByteArray(), token(context).toByteArray())
    }
}

/**
 * True when the strategy leaves the listen address to the app settings or binds it
 * to loopback. A strategy from another app must not expose the proxy to the network.
 */
fun strategyListensOnLoopback(strategy: String): Boolean {
    val (host, _) = shellSplit(strategy).checkIpAndPortInArgs()
    return host == null || isLoopbackHost(host)
}

fun isLoopbackHost(host: String): Boolean {
    val address = host.trim().removeSurrounding("[", "]")
    if (address.equals("localhost", ignoreCase = true)) return true
    if (Regex("""127(\.\d{1,3}){3}""").matches(address)) return true
    // IPv6 literals always contain ':', so this never triggers a DNS lookup.
    if (':' !in address) return false
    return try {
        InetAddress.getByName(address).isLoopbackAddress
    } catch (_: Exception) {
        false
    }
}
