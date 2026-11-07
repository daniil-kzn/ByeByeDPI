package io.github.romanvht.byedpi.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import io.github.romanvht.byedpi.R
import io.github.romanvht.byedpi.data.AppStatus
import io.github.romanvht.byedpi.data.Mode
import io.github.romanvht.byedpi.data.RESTART_ACTION
import io.github.romanvht.byedpi.data.START_ACTION
import io.github.romanvht.byedpi.data.STOP_ACTION
import io.github.romanvht.byedpi.receiver.ServiceActionReceiver
import io.github.romanvht.byedpi.services.ServiceManager
import io.github.romanvht.byedpi.services.appStatus
import io.github.romanvht.byedpi.utility.ExternalControl
import io.github.romanvht.byedpi.utility.getCmdArgs
import io.github.romanvht.byedpi.utility.getPreferences
import io.github.romanvht.byedpi.utility.mode
import io.github.romanvht.byedpi.utility.strategyListensOnLoopback

class ToggleActivity : Activity() {

    companion object {
        private const val TAG = "ToggleServiceActivity"
        const val EXTRA_STRATEGY = "strategy"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceManager.refresh(this)

        prefs = getPreferences()
        val trusted = ExternalControl.isTrusted(this, intent.getStringExtra(ExternalControl.EXTRA_TOKEN))
        if (!trusted && !prefs.getBoolean(ExternalControl.PREF_ALLOW_CONTROL, true)) {
            Log.w(TAG, "External control is disabled, request ignored")
            finish()
            return
        }

        val strategy = intent.getStringExtra(EXTRA_STRATEGY)
        if (strategy != null && !trusted && !isExternalStrategyAllowed(strategy)) {
            Toast.makeText(this, R.string.external_strategy_rejected, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val updated = updateStrategy(strategy)

        val onlyUpdate = intent.getBooleanExtra("only_update", false)
        val onlyStart = intent.getBooleanExtra("only_start", false)
        val onlyStop = intent.getBooleanExtra("only_stop", false)

        when {
            onlyUpdate -> {
                Log.i(TAG, "Only update strategy")
            }
            onlyStart -> {
                val (status) = appStatus
                if (status == AppStatus.Halted) {
                    startService()
                } else {
                    Log.i(TAG, "Service already running")
                }
            }
            onlyStop -> {
                val (status) = appStatus
                if (status == AppStatus.Running) {
                    stopService()
                } else {
                    Log.i(TAG, "Service already stopped")
                }
            }
            else -> {
                toggleService(updated)
            }
        }

        finish()
    }

    private fun startService() {
        val mode = prefs.mode()

        if (mode == Mode.VPN && VpnService.prepare(this) != null) {
            return
        }

        sendServiceAction(START_ACTION, mode)
        Log.i(TAG, "Toggle service start")
    }

    private fun restartService() {
        val mode = prefs.mode()

        if (mode == Mode.VPN && VpnService.prepare(this) != null) {
            return
        }

        sendServiceAction(RESTART_ACTION, mode)
        Log.i(TAG, "Toggle service start")
    }

    private fun stopService() {
        sendServiceAction(STOP_ACTION, appStatus.second)
        Log.i(TAG, "Toggle service stop")
    }

    private fun sendServiceAction(action: String, mode: Mode) {
        sendBroadcast(Intent(this, ServiceActionReceiver::class.java).setAction(action)
            .putExtra(ServiceActionReceiver.EXTRA_MODE, mode.name))
    }

    private fun toggleService(restart: Boolean) {
        val (status) = appStatus
        when (status) {
            AppStatus.Halted -> {
                startService()
            }
            AppStatus.Running -> {
                if (restart) {
                    restartService()
                } else {
                    stopService()
                }
            }
        }
    }

    private fun isExternalStrategyAllowed(strategy: String): Boolean {
        if (!prefs.getBoolean(ExternalControl.PREF_ALLOW_STRATEGY, false)) {
            Log.w(TAG, "Strategy change from another app is disabled")
            return false
        }
        if (!strategyListensOnLoopback(strategy)) {
            Log.w(TAG, "Strategy from another app binds the proxy to a non-loopback address")
            return false
        }
        return true
    }

    private fun updateStrategy(strategy: String?): Boolean {
        val current = prefs.getCmdArgs()
        if (strategy != null && strategy != current) {
            prefs.edit(commit = true) { putString("byedpi_cmd_args", strategy) }
            Log.i(TAG, "Strategy updated to: $strategy")
            return true
        }
        return false
    }
}
