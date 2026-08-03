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
 * Public Environment API for plugin use.
 *
 * Exposes IDE path constants that plugins legitimately need (e.g. to locate
 * the projects directory or the SDK). Raw [IDEEnvironment] is never reachable
 * from plugin code.
 *
 * All paths are read-only from the plugin perspective.
 * Obtain an instance via [PluginApi.environment].
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface IEnvironmentApi {

    /**
     * The directory of the project currently open in the editor.
     * Null when called outside of EditorActivity or before a project is loaded.
     */
    val openProjectDir: File?

    /**
     * ~/AndroidCSProjects on external storage.
     */
    val projectsDir: File

    /**
     * $HOME/AndroidCSProjects inside IDE internal storage.
     */
    val acsRootProjects: File

    /**
     * $HOME/Android/Sdk
     */
    val androidSdkDir: File

    /**
     * $HOME/flutter
     */
    val flutterDir: File

    /**
     * Context.filesDir
     */
    val filesDir: File

    /**
     * $filesDir/home
     */
    val homeDir: File

    /**
     * $filesDir/localenv
     */
    val localDir: File

    /**
     * $localDir/tmp
     */
    val tmpDir: File

    /**
     * Android SDK path as seen inside the rootfs.
     */
    val rootfsAndroidSdkPath: String

    /**
     * JAVA_HOME inside the rootfs.
     */
    val rootfsJavaHome: String

    /**
     * True when the IDE environment has been fully initialised.
     */
    fun isInitialized(): Boolean

    /**
     * Returns the full environment variable map the IDE uses for subprocess
     * invocations, optionally merged with caller-supplied overrides.
     *
     * @param additionalEnv Additional environment variables to merge.
     * @return The complete environment map.
     */
    fun getEnvironment(additionalEnv: Map<String, String> = emptyMap()): Map<String, String>
}
