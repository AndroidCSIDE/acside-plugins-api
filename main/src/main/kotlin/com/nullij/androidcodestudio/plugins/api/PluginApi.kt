package com.nullij.androidcodestudio.plugins.api

import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi

/**
 * Single entry point for all plugin ↔ IDE interaction.
 *
 * ─── For plugin developers ───────────────────────────────────────────────────
 *
 *   val editor = PluginApi.editor          // IEditorApi
 *   val env    = PluginApi.environment     // IEnvironmentApi
 *   val lsp    = PluginApi.lsp             // ILspApi?  (null outside EditorActivity)
 *   val tmpls  = PluginApi.templates       // ITemplateApi
 *   val proc   = PluginApi.process         // IProcessApi
 *   val ui     = PluginApi.ui              // IUiApi  | show Compose overlays
 *
 * All accessors throw [IllegalStateException] if called before the IDE has
 * initialised the API (which happens before any plugin action fires).
 *
 */
object PluginApi {

    @Volatile private var _editor: IEditorApi?           = null
    @Volatile private var _environment: IEnvironmentApi? = null
    @Volatile private var _lsp: ILspApi?                 = null
    @Volatile private var _templates: ITemplateApi?      = null
    @Volatile private var _process: IProcessApi?         = null
    @Volatile private var _ui: IUiApi?                   = null

    @Volatile private var _wired = false

    // ─── Public accessors (plugin-facing) ────────────────────────────────────

    val editor: IEditorApi
        get() = _editor ?: notInitialised("editor")

    val environment: IEnvironmentApi
        get() = _environment ?: notInitialised("environment")

    /**
     * Null when invoked outside of EditorActivity (e.g. from the home screen).
     * Always check for null before use.
     */
    val lsp: ILspApi?
        get() = _lsp

    val templates: ITemplateApi
        get() = _templates ?: notInitialised("templates")

    val process: IProcessApi
        get() = _process ?: notInitialised("process")

    /**
     * Show arbitrary Compose UI overlaid on the editor.
     * Null outside of EditorActivity | always null-check before use.
     *
     * Usage:
     *   PluginApi.ui?.showOverlay { handle ->
     *       MyDialog(onDismiss = { handle.dismiss() })
     *   }
     */
    val ui: IUiApi?
        get() = _ui

    // ─── IDE-internal wiring (NOT for plugin developers) ─────────────────────

    /**
     * FIX #2: The entire method is now @Synchronized to prevent a race where
     * two concurrent callers both read _wired == false, both pass the guard,
     * and interleave their field writes.  @Volatile alone does not help here
     * because the check-then-act is not atomic.
     */
    @InternalPluginApi
    @Synchronized
    fun wire(
        editor:      IEditorApi,
        environment: IEnvironmentApi,
        lsp:         ILspApi?,
        templates:   ITemplateApi,
        process:     IProcessApi,
        ui:          IUiApi? = null
    ) {
        enforceCallerIsNotPlugin()

        if (_wired) return  // One-shot: ignore subsequent calls.

        _editor      = editor
        _environment = environment
        _lsp         = lsp
        _templates   = templates
        _process     = process
        _ui          = ui
        _wired       = true
    }

    @InternalPluginApi
    @Synchronized
    fun clearLsp() {
        enforceCallerIsNotPlugin()
        _lsp = null
    }

    /**
     * Clear the UI API when EditorActivity is destroyed so plugins can't
     * show overlays into a dead window.
     */
    @InternalPluginApi
    @Synchronized
    fun clearUi() {
        enforceCallerIsNotPlugin()
        _ui = null
    }

    @InternalPluginApi
    @Synchronized
    fun reset() {
        enforceCallerIsNotPlugin()
        _editor      = null
        _environment = null
        _lsp         = null
        _templates   = null
        _process     = null
        _ui          = null
        _wired       = false
    }

    // ─── Runtime caller guard ────────────────────────────────────────────────

    /**
     * FIX #1: The original implementation had an inverted classloader check.
     * It used Class.forName(..., systemClassLoader) on each stack frame.
     * Plugin classes are NOT loadable by the system ClassLoader, so getOrNull()
     * returned null for every plugin frame and `continue` silently skipped them —
     * making the guard a no-op against actual plugin callers.
     *
     * Corrected logic:
     *  - Obtain the IDE's own ClassLoader from PluginApi itself (trusted anchor).
     *  - For the thread's context ClassLoader: reject if it is NOT in the trusted
     *    loader's ancestry chain (plugin ClassLoaders are siblings, not ancestors).
     *  - For each non-JVM stack frame: reject if the class CANNOT be loaded by
     *    the trusted IDE ClassLoader (plugin classes are invisible to it).
     *    JVM / Android framework / Kotlin runtime frames are whitelisted by
     *    package prefix and do not trigger false positives.
     */
    private fun enforceCallerIsNotPlugin() {
        val ideLoader = PluginApi::class.java.classLoader
            ?: throw SecurityException("PluginApi: cannot determine trusted ClassLoader.")

        // ── 1. Thread-context ClassLoader check ──────────────────────────────
        // The IDE sets the plugin's ClassLoader as the thread context CL when
        // dispatching into plugin code.  That loader is a sibling of ideLoader,
        // not an ancestor.  Reject if the context CL is not in ideLoader's chain.
        val ctxLoader = Thread.currentThread().contextClassLoader
        if (ctxLoader != null && !isLoaderTrusted(ideLoader, ctxLoader)) {
            throw SecurityException(
                "PluginApi.wire()/reset()/clearLsp()/clearUi() may not be called from plugin code."
            )
        }

        // ── 2. Stack-frame check ─────────────────────────────────────────────
        // Try to load each frame's class with the trusted IDE ClassLoader.
        // If it succeeds, the class belongs to the IDE — allow it.
        // If it fails, the class belongs to an external (plugin) ClassLoader — reject.
        // JVM / Android / Kotlin frames are skip-listed to avoid false positives
        // from classes that are simply not part of the app at all.
        for (frame in Thread.currentThread().stackTrace) {
            val cn = frame.className
            if (isJvmOrFrameworkClass(cn)) continue

            val loadedByIde = runCatching {
                Class.forName(cn, false, ideLoader)
            }.isSuccess

            if (!loadedByIde) {
                throw SecurityException(
                    "PluginApi internal API called from external (plugin) class: $cn"
                )
            }
        }
    }

    /**
     * Returns true if [candidate] is [trusted] or a descendant of it — i.e.
     * [trusted] appears somewhere in [candidate]'s parent-ClassLoader chain.
     */
    private fun isLoaderTrusted(trusted: ClassLoader, candidate: ClassLoader): Boolean {
        var cur: ClassLoader? = candidate
        while (cur != null) {
            if (cur === trusted) return true
            cur = cur.parent
        }
        return false
    }

    /**
     * True for JVM internals, Android framework, and Kotlin runtime classes
     * that are never plugin code but also aren't loadable by the IDE ClassLoader.
     * Keeping this list tight avoids false-positive rejections.
     */
    private fun isJvmOrFrameworkClass(className: String): Boolean =
        className.startsWith("java.")
            || className.startsWith("javax.")
            || className.startsWith("kotlin.")
            || className.startsWith("kotlinx.")
            || className.startsWith("android.")
            || className.startsWith("androidx.")
            || className.startsWith("dalvik.")
            || className.startsWith("sun.")
            || className.startsWith("com.android.")
            || className.startsWith("libcore.")

    private fun notInitialised(name: String): Nothing =
        error("PluginApi.$name is not available. Ensure the IDE has initialised the plugin API before this action fires.")
}
