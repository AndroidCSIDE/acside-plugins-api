package com.nullij.androidcodestudio.plugins.api

import android.content.Context
import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi
import java.io.File

/**
 * Public Template API for plugin use.
 *
 * Plugins register project/file templates through this interface.
 * Obtain an instance via [PluginApi.templates].
 */
interface ITemplateApi {

    /**
     * Register a new template with the IDE's template system.
     *
     * @param spec   A [TemplateSpec] describing the template and its creation logic.
     * @return An opaque handle that can be passed to [unregisterTemplate].
     */
    fun registerTemplate(spec: TemplateSpec): TemplateHandle

    /**
     * Unregister a previously registered template.
     * The template will no longer appear in the New Project / New File wizard.
     */
    fun unregisterTemplate(handle: TemplateHandle)

    // ─── File / directory helpers ─────────────────────────────────────────────

    /** Create [paths] as a chain of directories under [baseDir]. Returns the leaf dir. */
    fun createDirectories(baseDir: File, vararg paths: String): File

    /** Write [content] to [fileName] inside [dir]. Returns the created [File]. */
    fun createFile(dir: File, fileName: String, content: String): File

    /**
     * Build a standard Android project directory structure under [projectDir].
     * Returns a [ProjectStructure] with all common paths pre-populated.
     */
    fun createStandardStructure(projectDir: File, packageId: String): ProjectStructure

    // ─── Options helper ───────────────────────────────────────────────────────

    /**
     * Extract typed [TemplateOptionsData] from the raw options object the IDE
     * passes to a template's create() method.
     *
     * Use this when your plugin is invoked via templates.json (i.e. you receive
     * a raw Any options parameter rather than a pre-typed [TemplateOptionsData]).
     */
    fun extractOptions(rawOptions: Any): TemplateOptionsData

    @InternalPluginApi
    fun callListenerMethod(listener: Any?, methodName: String, vararg args: Any?)
}

// ─── Supporting types (plugin-visible) ───────────────────────────────────────

/**
 * Describes a template that a plugin wants to contribute to the IDE.
 *
 * @param displayName   Shown in the template picker UI.
 * @param templateType  "ACTIVITY", "FRAGMENT", "PROJECT", etc.
 * @param onCreate      Suspend lambda called when the user creates from this template.
 *                      Receives a [PluginContext], the typed [TemplateOptionsData],
 *                      and the raw options object for reading extra fields via
 *                      [ITemplateApi.extractOptions].
 */
data class TemplateSpec(
    val displayName: String,
    val templateType: String = "ACTIVITY",
    val onCreate: suspend (
        context: PluginContext,
        options: TemplateOptionsData,
        rawOptions: Any
    ) -> Unit
)

/** Opaque handle returned by [ITemplateApi.registerTemplate]. */
class TemplateHandle @InternalPluginApi constructor(@InternalPluginApi val id: String)

/**
 * Typed representation of the common fields from a template options object.
 */
data class TemplateOptionsData(
    val projectName: String,
    val packageId: String,
    val minSdk: Int,
    val useKts: Boolean,
    val saveLocation: File,
    val languageType: String
)

/**
 * Standard Android project directory structure returned by
 * [ITemplateApi.createStandardStructure].
 */
data class ProjectStructure(
    val projectDir:  File,
    val mainSrcDir:  File,
    val javaDir:     File,
    val resDir:      File,
    val layoutDir:   File,
    val valuesDir:   File,
    val drawableDir: File,
    val manifestFile: File,
    val packageId:   String,
    val packagePath: String
)
