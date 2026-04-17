package com.nullij.androidcodestudio.plugins.api

import java.io.File

/**
 * Public Environment API for plugin use.
 *
 * Exposes IDE path constants that plugins legitimately need (e.g. to locate
 * the projects directory or the SDK).  Raw [IDEEnvironment] is never reachable
 * from plugin code.
 *
 * All paths are read-only from the plugin perspective.
 * Obtain an instance via [PluginApi.environment].
 */
interface IEnvironmentApi {

    // ─── Project directories ─────────────────────────────────────────────────

    /**
     * The directory of the project currently open in the editor.
     * Null when called outside of EditorActivity or before a project is loaded.
     */
    val openProjectDir: File?
    
    /** ~/AndroidCSProjects on external storage. */
    val projectsDir: File

    /** $HOME/AndroidCSProjects inside IDE internal storage. */
    val acsRootProjects: File

    // ─── SDK / toolchain ─────────────────────────────────────────────────────

    /** $HOME/Android/Sdk */
    val androidSdkDir: File

    /** $HOME/flutter */
    val flutterDir: File

    // ─── App-private directories ─────────────────────────────────────────────

    /** Context.filesDir */
    val filesDir: File

    /** $filesDir/home */
    val homeDir: File

    /** $filesDir/localenv */
    val localDir: File

    /** $localDir/tmp */
    val tmpDir: File

    // ─── Rootfs paths (string, as seen inside proot) ─────────────────────────

    /** Android SDK path as seen inside the rootfs. */
    val rootfsAndroidSdkPath: String

    /** JAVA_HOME inside the rootfs. */
    val rootfsJavaHome: String

    // ─── Status ──────────────────────────────────────────────────────────────

    /** True when the IDE environment has been fully initialised. */
    fun isInitialized(): Boolean

    /**
     * Returns the full environment variable map the IDE uses for subprocess
     * invocations, optionally merged with caller-supplied overrides.
     */
    fun getEnvironment(additionalEnv: Map<String, String> = emptyMap()): Map<String, String>
}
