package io.github.romanvht.byedpi.utility

import android.system.Os
import android.system.OsConstants
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import java.net.InetAddress

private const val TAG = "ValidateUtils"

/**
 * Parses a numeric IPv4 or IPv6 address without DNS. `Os.inet_pton` exists since
 * API 21, so validation works on every supported version, not only on API 29+.
 */
private fun parseNumericAddress(ip: String): InetAddress? = try {
    Os.inet_pton(OsConstants.AF_INET, ip) ?: Os.inet_pton(OsConstants.AF_INET6, ip)
} catch (_: Exception) {
    null
}

fun checkIp(ip: String): Boolean = parseNumericAddress(ip) != null

fun checkNotLocalIp(ip: String): Boolean =
    parseNumericAddress(ip)?.let { !it.isAnyLocalAddress && !it.isLoopbackAddress } ?: false

fun checkDomain(domain: String): Boolean {
    if (domain.isEmpty()) return false

    if (domain.length > 253) return false
    if (domain.startsWith(".") || domain.endsWith(".")) return false
    if (!domain.contains(".")) return false

    return true
}

fun PreferenceFragmentCompat.setEditTestPreferenceListenerDomain(key: String) {
    setEditTextPreferenceListener(key) { value ->
        value.isNotEmpty() && checkDomain(value)
    }
}

fun PreferenceFragmentCompat.setEditTestPreferenceListenerPort(key: String) {
    setEditTestPreferenceListenerInt(key, 1, 65535)
}

fun PreferenceFragmentCompat.setEditTestPreferenceListenerInt(
    key: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE
) {
    setEditTextPreferenceListener(key) { value ->
        value.toIntOrNull()?.let { it in min..max } ?: false
    }
}

fun PreferenceFragmentCompat.setEditTextPreferenceListener(
    key: String,
    check: (String) -> Boolean
) {
    findPreferenceNotNull<EditTextPreference>(key)
        .setOnPreferenceChangeListener { preference, newValue ->
            when (newValue) {
                is String -> {
                    val valid = check(newValue)
                    if (!valid) {
                        Toast.makeText(
                            requireContext(),
                            "Invalid value for ${preference.title}: $newValue",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    valid
                }

                else -> {
                    Log.w(
                        TAG,
                        "Invalid type for ${preference.key}: " +
                                "$newValue has type ${newValue::class.java}"
                    )
                    false
                }
            }
        }
}
