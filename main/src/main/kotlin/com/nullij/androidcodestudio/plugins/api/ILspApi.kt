package com.nullij.androidcodestudio.plugins.api

import java.io.File

/**
 * Public LSP (Language Server Protocol) API for plugin use.
 *
 * Plugins can query and drive the IDE's language servers through this
 * interface without ever touching the internal LanguageServerRegistry.
 *
 * Obtain an instance via [PluginApi.lsp].
 * Returns null from [PluginApi.lsp] when called outside of EditorActivity.
 */
interface ILspApi {

    // ─── Server status ───────────────────────────────────────────────────────

    /** True if a server for [languageId] is registered (may or may not be running). */
    fun hasServer(languageId: String): Boolean

    /** True if the server for [languageId] is currently running. */
    fun isServerRunning(languageId: String): Boolean

    /** Set of language IDs that have registered servers. */
    fun getAvailableServers(): Set<String>

    /** Set of language IDs whose servers are currently active. */
    fun getRunningServers(): Set<String>

    // ─── Server lifecycle ────────────────────────────────────────────────────

    /**
     * Start the server for [languageId].
     * Returns true if the server started successfully or was already running.
     */
    fun startServer(languageId: String): Boolean

    /** Stop the server for [languageId]. No-op if not running. */
    fun stopServer(languageId: String)

    /** Stop all running servers. */
    fun stopAllServers()

    // ─── Document events ─────────────────────────────────────────────────────

    /**
     * Notify the registry that [file] has been opened.
     * The registry will start the appropriate server if not already running.
     */
    fun openDocument(file: File): Boolean

    /** Notify the registry that [file] has been closed. */
    fun closeDocument(file: File)

    /**
     * Notify the registry that [file] content has changed.
     * [version] must be monotonically increasing for the same file.
     */
    fun documentChanged(file: File, content: String, version: Int)

    // ─── Language detection ──────────────────────────────────────────────────

    /** Detect the language ID for the given [file]. Returns null if unrecognised. */
    fun detectLanguage(file: File): String?

    /** Detect the language ID from a [fileName] (including extension). */
    fun detectLanguage(fileName: String): String?

    // ─── Extension registration ──────────────────────────────────────────────

    /**
     * Register a custom plugin language server.
     *
     * [server] must implement [PluginLanguageServerSpec].
     */
    fun registerServer(languageId: String, server: PluginLanguageServerSpec)

    /** Unregister the server for [languageId]. */
    fun unregisterServer(languageId: String)

    /**
     * Associate a file extension (e.g. "kt", "py") with a [languageId].
     * This controls which server receives document events for that file type.
     */
    fun registerExtension(extension: String, languageId: String)

    fun unregisterExtension(extension: String)

    /** Returns the current extension → languageId map. */
    fun getRegisteredExtensions(): Map<String, String>
}

/**
 * Plugin authors implement this on the class that you want to wrap your LSP client:
 *
 *   class MyLspClient : LanguageServerClient { ... }
 */
interface LanguageServerClient

/**
 * Public specification for a plugin-provided language server.
 *
 * Plugin authors implement this interface.  The gate layer wraps it in the
 * internal PluginLanguageServer proxy before registering with the registry.
 */
interface PluginLanguageServerSpec {
    val languageId: String

    fun start(): Boolean
    fun stop()
    fun isRunning(): Boolean

    /**
     * FIX #10: Return type changed from [Any] to [LanguageServerClient].
     * Implement this by returning a [LanguageServerClient] wrapper around
     * your actual LSP client instance.
     */
    fun getClient(): LanguageServerClient

    /** Called when a file is opened in the editor. Return true on success. */
    fun openDocument(file: java.io.File): Boolean

    /** Called when a file is closed in the editor. */
    fun closeDocument(file: java.io.File)

    /**
     * Called when a file's content changes.
     * @param version monotonically increasing version counter for this file.
     */
    fun documentChanged(file: java.io.File, content: String, version: Int)
}
