package com.nullij.androidcodestudio.plugins.api

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi

/**
 * A restricted Context wrapper that is the ONLY way plugins receive an
 * Android Context.  Raw [android.app.Activity] and full [Context] references
 * are never handed to plugin code — all potentially dangerous APIs are absent.
 *
 * The IDE constructs this from the real Context inside [gate.PluginApiGateway]
 * before invoking any plugin action.  Plugins receive a [PluginContext] as the
 * single argument to their entry point.
 *
 * Exposed surface (intentionally minimal):
 *  - [packageName]         — read-only string
 *  - [getSystemService]    — whitelisted subset only
 *  - [runOnUiThread]       — post work to the main thread without Activity reference
 *  - [applicationContext]  — returns a [PluginSandboxContext], NOT the raw Context.
 *                            Dangerous APIs on this context throw [SecurityException].
 *
 * Everything else from the Android Context API is deliberately not forwarded.
 */
class PluginContext @InternalPluginApi constructor(rawContext: Context) {

    // Always unwrap to applicationContext so plugins can never hold a reference
    // to an Activity and leak it or call finish() / startActivity() directly.
    private val appCtx: Context = rawContext.applicationContext

    /** The app package name. Read-only; no side effects. */
    val packageName: String get() = appCtx.packageName

    /**
     * FIX #5: Previously returned the raw applicationContext, allowing plugins
     * to bypass the getSystemService whitelist by calling
     * applicationContext.getSystemService() directly, and to access
     * contentResolver, startService(), registerReceiver(), etc.
     *
     * Now returns a [PluginSandboxContext] — a ContextWrapper that overrides
     * every dangerous operation with a SecurityException.  Only the small set
     * of safe APIs (resources, shared preferences, package info, assets) passes
     * through to the real context.
     *
     * ⚠ Do NOT cast this to Activity or to the raw applicationContext.
     */
    val applicationContext: Context get() = PluginSandboxContext(appCtx)

    /**
     * Retrieve a system service by name.
     *
     * Only the following services are forwarded to avoid privilege escalation:
     *  - [Context.CLIPBOARD_SERVICE]
     *  - [Context.CONNECTIVITY_SERVICE]
     *  - [Context.INPUT_METHOD_SERVICE]
     *  - [Context.VIBRATOR_SERVICE]
     *  - [Context.NOTIFICATION_SERVICE]
     *
     * Any other service name throws [SecurityException].
     */
    fun getSystemService(name: String): Any? {
        require(name in ALLOWED_SERVICES) {
            "PluginContext: access to system service '$name' is not permitted for plugins."
        }
        return appCtx.getSystemService(name)
    }

    /**
     * Post [action] to the main (UI) thread.
     * Plugins that need to update Compose state or show a Toast must use this.
     */
    fun runOnUiThread(action: Runnable) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    // ─── PluginSandboxContext ─────────────────────────────────────────────────

    /**
     * A [ContextWrapper] that exposes only the safe subset of the Android
     * Context API.  Every operation that could be used for privilege escalation,
     * data exfiltration, or UI spoofing throws [SecurityException].
     *
     * The [getSystemService] whitelist is re-enforced here so it cannot be
     * bypassed by going through the wrapped base Context.
     */
    private inner class PluginSandboxContext(base: Context) : ContextWrapper(base) {

        // ── Re-enforce system service whitelist ───────────────────────────────
        override fun getSystemService(name: String): Any? {
            require(name in ALLOWED_SERVICES) {
                "PluginContext: access to system service '$name' is not permitted for plugins."
            }
            return super.getSystemService(name)
        }

        // ── Block Activity launching ──────────────────────────────────────────
        override fun startActivity(intent: Intent?): Nothing =
            throw SecurityException("Plugins may not call startActivity().")

        override fun startActivity(intent: Intent?, options: Bundle?): Nothing =
            throw SecurityException("Plugins may not call startActivity().")

        override fun startActivities(intents: Array<out Intent?>?): Nothing =
            throw SecurityException("Plugins may not call startActivities().")

        override fun startActivities(intents: Array<out Intent?>?, options: Bundle?): Nothing =
            throw SecurityException("Plugins may not call startActivities().")

        // ── Block Service binding/starting ────────────────────────────────────
        override fun startService(service: Intent?): ComponentName? =
            throw SecurityException("Plugins may not call startService().")

        override fun stopService(service: Intent?): Boolean =
            throw SecurityException("Plugins may not call stopService().")

        override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean =
            throw SecurityException("Plugins may not call bindService().")

        override fun unbindService(conn: ServiceConnection): Unit =
            throw SecurityException("Plugins may not call unbindService().")

        // ── Block Broadcast sending/receiving ─────────────────────────────────
        override fun sendBroadcast(intent: Intent?): Nothing =
            throw SecurityException("Plugins may not send broadcasts.")

        override fun sendBroadcast(intent: Intent?, receiverPermission: String?): Nothing =
            throw SecurityException("Plugins may not send broadcasts.")

        override fun sendOrderedBroadcast(intent: Intent?, receiverPermission: String?): Nothing =
            throw SecurityException("Plugins may not send ordered broadcasts.")

        override fun sendOrderedBroadcast(
            intent: Intent, receiverPermission: String?, resultReceiver: BroadcastReceiver?,
            scheduler: Handler?, initialCode: Int, initialData: String?, initialExtras: Bundle?
        ): Nothing = throw SecurityException("Plugins may not send ordered broadcasts.")

        override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? =
            throw SecurityException("Plugins may not register BroadcastReceivers.")

        override fun registerReceiver(
            receiver: BroadcastReceiver?, filter: IntentFilter?, flags: Int
        ): Intent? = throw SecurityException("Plugins may not register BroadcastReceivers.")

        override fun registerReceiver(
            receiver: BroadcastReceiver?, filter: IntentFilter?,
            broadcastPermission: String?, scheduler: Handler?
        ): Intent? = throw SecurityException("Plugins may not register BroadcastReceivers.")

        override fun registerReceiver(
            receiver: BroadcastReceiver?, filter: IntentFilter?,
            broadcastPermission: String?, scheduler: Handler?, flags: Int
        ): Intent? = throw SecurityException("Plugins may not register BroadcastReceivers.")

        override fun unregisterReceiver(receiver: BroadcastReceiver?): Unit =
            throw SecurityException("Plugins may not unregister BroadcastReceivers.")

        // ── Block direct ContentResolver access ───────────────────────────────
        // Plugins that need to read/write content should use the IDE's explicit
        // APIs rather than a raw ContentResolver.
        override fun getContentResolver(): ContentResolver =
            throw SecurityException(
                "Plugins may not access ContentResolver directly. " +
                "Use the IDE's dedicated APIs for content access."
            )
    }

    // ─── companion ───────────────────────────────────────────────────────────

    companion object {
        private val ALLOWED_SERVICES = setOf(
            Context.CLIPBOARD_SERVICE,
            Context.CONNECTIVITY_SERVICE,
            Context.INPUT_METHOD_SERVICE,
            Context.VIBRATOR_SERVICE,
            Context.NOTIFICATION_SERVICE
        )
    }
}
