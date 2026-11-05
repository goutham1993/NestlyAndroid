package com.lifestyle.nestly.widget

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Fulfillment target for Google Assistant / App Actions "add feed" phrases.
 * Enqueues the same one-tap bottle save as the widget Bottle button, shows no
 * UI, and finishes immediately so the full app never opens.
 */
class QuickBottleLogActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Capture voice/invocation time before any async work, matching the
        // widget callback's "tap time, not worker start time" behavior.
        val atMs = System.currentTimeMillis()
        // applicationContext so the "Saving…" toast outlives this translucent shell.
        val appContext = applicationContext
        scope.launch {
            try {
                enqueueQuickBottleLog(appContext, atMs = atMs)
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
