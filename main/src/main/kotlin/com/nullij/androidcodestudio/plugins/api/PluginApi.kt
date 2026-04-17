package com.nullij.androidcodestudio.plugins.api

import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi

/**
 * Single entry point for all plugin ↔ IDE interaction.
 *
 *   val editor = PluginApi.editor          // IEditorApi
 *   val env    = PluginApi.environment     // IEnvironmentApi
 *   val lsp    = PluginApi.lsp             // ILspApi?  (null outside EditorActivity)
 *   val tmpls  = PluginApi.templates       // ITemplateApi
 *   val proc   = PluginApi.process         // IProcessApi
 *   val ui     = PluginApi.ui              // IUiApi
 *
 * All accessors throw [IllegalStateException] if called before the IDE has
 * initialised the API (which happens before any plugin action fires).
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
     * Null outside of EditorActivity, always null-check before use.
     *
     * Usage:
     *   PluginApi.ui?.showOverlay { handle ->
     *       MyDialog(onDismiss = { handle.dismiss() })
     *   }
     */
    val ui: IUiApi?
        get() = _ui

    // ─── IDE-internal wiring ──────────────────────────────────────────────────

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
        if (_wired) return
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

    // ─── Runtime caller guard ─────────────────────────────────────────────────

    private fun enforceCallerIsNotPlugin() {
        val ideLoader = PluginApi::class.java.classLoader
            ?: throw SecurityException("PluginApi: cannot determine trusted ClassLoader.")

        val ctxLoader = Thread.currentThread().contextClassLoader
        if (ctxLoader != null && !isLoaderTrusted(ideLoader, ctxLoader)) {
            throw SecurityException(
                "PluginApi.wire()/reset()/clearLsp()/clearUi() may not be called from plugin code."
            )
        }

        for (frame in Thread.currentThread().stackTrace) {
            val cn = frame.className
            if (isJvmOrFrameworkClass(cn)) continue
            val loadedByIde = runCatching { Class.forName(cn, false, ideLoader) }.isSuccess
            if (!loadedByIde) {
                throw SecurityException(
                    "PluginApi internal API called from external (plugin) class: $cn"
                )
            }
        }
    }

    /** Returns true if [candidate] is [trusted] or a descendant of it. */
    private fun isLoaderTrusted(trusted: ClassLoader, candidate: ClassLoader): Boolean {
        var cur: ClassLoader? = candidate
        while (cur != null) {
            if (cur === trusted) return true
            cur = cur.parent
        }
        return false
    }

    /** True for JVM, Android framework, and Kotlin runtime class name prefixes. */
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
