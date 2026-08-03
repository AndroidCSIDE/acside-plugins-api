/*
 *  This file is part of ACSIDE.
 *
 *  ACSIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ACSIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with ACSIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.nullij.androidcodestudio.plugins.api

import java.io.File

/**
 * Public LSP (Language Server Protocol) API for plugin use.
 *
 * Plugins can query and drive the IDE's language servers through this interface.
 * Obtain an instance via [PluginApi.lsp].
 * Returns null from [PluginApi.lsp] when called outside of EditorActivity.
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface ILspApi {

    /**
     * True if a server for [languageId] is registered (may or may not be running).
     */
    fun hasServer(languageId: String): Boolean

    /**
     * True if the server for [languageId] is currently running.
     */
    fun isServerRunning(languageId: String): Boolean

    /**
     * Set of language IDs that have registered servers.
     */
    fun getAvailableServers(): Set<String>

    /**
     * Set of language IDs whose servers are currently active.
     */
    fun getRunningServers(): Set<String>

    /**
     * Start the server for [languageId].
     * Returns true if the server started successfully or was already running.
     */
    fun startServer(languageId: String): Boolean

    /**
     * Stop the server for [languageId]. No-op if not running.
     */
    fun stopServer(languageId: String)

    /**
     * Stop all running servers.
     */
    fun stopAllServers()

    /**
     * Notify the registry that [file] has been opened.
     * The appropriate server will be started if not already running.
     *
     * @return True if the document was registered successfully.
     */
    fun openDocument(file: File): Boolean

    /**
     * Notify the registry that [file] has been closed.
     */
    fun closeDocument(file: File)

    /**
     * Notify the registry that [file] content has changed.
     *
     * @param version Must be monotonically increasing for the same file.
     */
    fun documentChanged(file: File, content: String, version: Int)

    /**
     * Detect the language ID for the given [file]. Returns null if unrecognised.
     */
    fun detectLanguage(file: File): String?

    /**
     * Detect the language ID from a [fileName] (including extension).
     */
    fun detectLanguage(fileName: String): String?

    /**
     * Register a custom plugin language server.
     *
     * @param server Must implement [PluginLanguageServerSpec].
     */
    fun registerServer(languageId: String, server: PluginLanguageServerSpec)

    /**
     * Unregister the server for [languageId].
     */
    fun unregisterServer(languageId: String)

    /**
     * Associate a file extension (e.g. "kt", "py") with a [languageId].
     * This controls which server receives document events for that file type.
     */
    fun registerExtension(extension: String, languageId: String)

    /**
     * Remove the association for the given file extension.
     */
    fun unregisterExtension(extension: String)

    /**
     * Returns the current extension → languageId map.
     */
    fun getRegisteredExtensions(): Map<String, String>
}

/**
 * Marker interface for plugin-provided LSP client wrappers.
 */
interface LanguageServerClient

/**
 * Public specification for a plugin-provided language server.
 *
 * Implement this interface and pass the instance to [ILspApi.registerServer].
 *
 * @author nullij @ https://github.com/nullij
 */
interface PluginLanguageServerSpec {

    /**
     * The language ID this server handles (e.g. "python", "rust").
     */
    val languageId: String

    /**
     * Start the server process. Return true on success.
     */
    fun start(): Boolean

    /**
     * Stop the server process gracefully.
     */
    fun stop()

    /**
     * Returns true if the server process is currently running.
     */
    fun isRunning(): Boolean

    /**
     * Return a [LanguageServerClient] wrapper around your actual LSP client instance.
     */
    fun getClient(): LanguageServerClient

    /**
     * Called when a file is opened in the editor.
     *
     * @return True on success.
     */
    fun openDocument(file: File): Boolean

    /**
     * Called when a file is closed in the editor.
     */
    fun closeDocument(file: File)

    /**
     * Called when a file's content changes.
     *
     * @param version Monotonically increasing version counter for this file.
     */
    fun documentChanged(file: File, content: String, version: Int)
}
