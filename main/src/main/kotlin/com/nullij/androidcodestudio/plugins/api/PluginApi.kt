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

import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi

/**
 * Single entry point for all plugin to IDE interaction.
 *
 * Accessors:
 *   val editor = PluginApi.editor          // IEditorApi
 *   val env    = PluginApi.environment     // IEnvironmentApi
 *   val lsp    = PluginApi.lsp             // ILspApi? (null outside EditorActivity)
 *   val tmpls  = PluginApi.templates       // ITemplateApi
 *   val proc   = PluginApi.process         // IProcessApi
 *   val ui     = PluginApi.ui              // IUiApi?
 *
 * All accessors throw [IllegalStateException] if called before the IDE has
 * initialised the API. Initialisation occurs before any plugin action fires.
 *
 * @author nullij
 * @see https://github.com/nullij
 */
object PluginApi {

    @Volatile private var _editor: IEditorApi?           = null
    @Volatile private var _environment: IEnvironmentApi? = null
    @Volatile private var _lsp: ILspApi?                 = null
    @Volatile private var _templates: ITemplateApi?      = null
    @Volatile private var _process: IProcessApi?         = null
    @Volatile private var _ui: IUiApi?                   = null

    @Volatile private var _wired = false

    val editor: IEditorApi
        get() = _editor ?: notInitialised("editor")

    val environment: IEnvironmentApi
        get() = _environment ?: notInitialised("environment")

    /**
     * Null when invoked outside of EditorActivity, e.g. from the home screen.
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
     *
     * Null outside of EditorActivity. Always null-check before use.
     *
     * Usage:
     *   PluginApi.ui?.showOverlay { handle ->
     *       MyDialog(onDismiss = { handle.dismiss() })
     *   }
     */
    val ui: IUiApi?
        get() = _ui

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
        _editor      = editor
        _environment = environment
        _lsp         = lsp
        _templates   = templates
        _process     = process
        _ui          = ui
        _wired       = true
    }

    /**
     * Null out the editor bridge so [EditorApiImpl] can be garbage collected.
     *
     * [EditorApiImpl] holds an Activity Context. This method allows it to be
     * GC'd as soon as [EditorActivity.onDestroy] fires.
     */
    @InternalPluginApi
    @Synchronized
    fun clearEditor() {
        enforceCallerIsNotPlugin()
        _editor = null
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
    fun clearProcess() {
        enforceCallerIsNotPlugin()
        _process = null
    }

    @InternalPluginApi
    @Synchronized
    fun clearTemplates() {
        enforceCallerIsNotPlugin()
        _templates = null
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

    private fun enforceCallerIsNotPlugin() {
        val ideLoader = PluginApi::class.java.classLoader
            ?: throw SecurityException("PluginApi: cannot determine trusted ClassLoader.")

        val stack = Thread.currentThread().stackTrace
        val selfClass = PluginApi::class.java.name
        var idx = 0
        while (idx < stack.size && stack[idx].className != selfClass) idx++
        while (idx < stack.size && stack[idx].className == selfClass) idx++
        while (idx < stack.size && isReflectionOrProxyFrame(stack[idx].className)) idx++

        val callerClassName = stack.getOrNull(idx)?.className
            ?: throw SecurityException("PluginApi: unable to determine caller.")

        val callerClass = runCatching { Class.forName(callerClassName, false, ideLoader) }
            .getOrElse {
                throw SecurityException(
                    "PluginApi internal API called from external (plugin) class: $callerClassName"
                )
            }

        val callerLoader = callerClass.classLoader
        if (callerLoader == null || !isLoaderTrusted(ideLoader, callerLoader)) {
            throw SecurityException(
                "PluginApi.wire()/reset()/clearLsp()/clearUi() may not be called from plugin code."
            )
        }
    }

    private fun isReflectionOrProxyFrame(className: String): Boolean =
        className.startsWith("java.lang.reflect.")
            || className.startsWith("jdk.internal.reflect.")
            || className.startsWith("kotlin.reflect.")
            || className.contains("\$Proxy")

    private fun isLoaderTrusted(trusted: ClassLoader, candidate: ClassLoader): Boolean {
        var cur: ClassLoader? = candidate
        while (cur != null) {
            if (cur === trusted) return true
            cur = cur.parent
        }
        return false
    }

    private fun notInitialised(name: String): Nothing =
        error("PluginApi.$name is not available. Ensure the IDE has initialised the plugin API before this action fires.")
}