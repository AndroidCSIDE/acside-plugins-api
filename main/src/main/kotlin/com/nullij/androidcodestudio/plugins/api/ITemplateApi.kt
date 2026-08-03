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

import android.content.Context
import com.nullij.androidcodestudio.plugins.api.gate.InternalPluginApi
import java.io.File

/**
 * Public API for registering and managing project and file templates.
 *
 * Plugins use this interface to contribute templates that appear in the IDE's
 * New Project and New File wizards. Templates define how files and directories
 * are created from user-provided options.
 *
 * Obtain an instance via [PluginApi.templates].
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface ITemplateApi {

    /**
     * Register a new template with the IDE's template system.
     *
     * @param spec A [TemplateSpec] describing the template and its creation logic.
     * @return An opaque handle that can be passed to [unregisterTemplate].
     */
    fun registerTemplate(spec: TemplateSpec): TemplateHandle

    /**
     * Unregister a previously registered template.
     *
     * After this call, the template no longer appears in the New Project or
     * New File wizard.
     *
     * @param handle The handle returned by [registerTemplate].
     */
    fun unregisterTemplate(handle: TemplateHandle)

    /**
     * Create a chain of directories under [baseDir].
     *
     * @param baseDir The root directory under which to create paths.
     * @param paths One or more directory names to create as a chain.
     * @return The leaf directory that was created.
     */
    fun createDirectories(baseDir: File, vararg paths: String): File

    /**
     * Write content to a file inside a directory.
     *
     * @param dir The parent directory.
     * @param fileName The name of the file to create.
     * @param content The content to write to the file.
     * @return The created [File] instance.
     */
    fun createFile(dir: File, fileName: String, content: String): File

    /**
     * Build a standard Android project directory structure under [projectDir].
     *
     * @param projectDir The root directory for the project.
     * @param packageId The package ID used to generate package paths.
     * @return A [ProjectStructure] containing all common paths.
     */
    fun createStandardStructure(projectDir: File, packageId: String): ProjectStructure

    /**
     * Extract typed [TemplateOptionsData] from the raw options object.
     *
     * Use this when your plugin is invoked via templates.json. In that case,
     * you receive a raw Any options parameter rather than a pre-typed
     * [TemplateOptionsData].
     *
     * @param rawOptions The raw options object from the IDE.
     * @return A typed [TemplateOptionsData] instance.
     */
    fun extractOptions(rawOptions: Any): TemplateOptionsData

    /**
     * Call a method on a listener object via reflection.
     *
     * Internal helper for bridging between plugin code and IDE listeners.
     *
     * @param listener The listener object, or null.
     * @param methodName The name of the method to call.
     * @param args The arguments to pass to the method.
     */
    fun callListenerMethod(listener: Any?, methodName: String, vararg args: Any?)
}

/**
 * Describes a template that a plugin wants to contribute to the IDE.
 *
 * @param displayName Shown in the template picker UI.
 * @param templateType The type of template, e.g. "ACTIVITY", "FRAGMENT", or "PROJECT".
 * @param onCreate Suspend lambda called when the user creates from this template.
 *                 Receives a [PluginContext], the typed [TemplateOptionsData],
 *                 and the raw options object for reading extra fields via
 *                 [ITemplateApi.extractOptions].
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

/**
 * Opaque handle returned by [ITemplateApi.registerTemplate].
 */
class TemplateHandle @InternalPluginApi constructor(@InternalPluginApi val id: String)

/**
 * Typed representation of the common fields from a template options object.
 *
 * @param projectName The name of the project.
 * @param packageId The package ID for the project.
 * @param minSdk The minimum SDK version.
 * @param useKts Whether to use Kotlin script (.kts) files.
 * @param saveLocation The directory where the project is saved.
 * @param languageType The programming language selected.
 * @param customFields Additional fields provided by the template.
 */
data class TemplateOptionsData(
    val projectName: String,
    val packageId: String,
    val minSdk: Int,
    val useKts: Boolean,
    val saveLocation: File,
    val languageType: String,
    val customFields: Map<String, String> = emptyMap(),
)

/**
 * Standard Android project directory structure.
 *
 * Returned by [ITemplateApi.createStandardStructure].
 *
 * @param projectDir The root project directory.
 * @param mainSrcDir The main source directory (e.g. app/src/main).
 * @param javaDir The Java/Kotlin source directory.
 * @param resDir The resources directory.
 * @param layoutDir The layout resources directory.
 * @param valuesDir The values resources directory.
 * @param drawableDir The drawable resources directory.
 * @param manifestFile The AndroidManifest.xml file.
 * @param packageId The package ID of the project.
 * @param packagePath The package path as a file path string.
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