package com.nullij.androidcodestudio.plugins.api

/**
 * Public API for launching managed processes inside the IDE's proot/acsenv environment.
 *
 * Plugin language servers use this to start their server executable. The IDE
 * handles environment setup, storage binding, and process lifecycle | the plugin
 * just describes what to run.
 *
 * Usage:
 *
 *   val process = PluginApi.process
 *       .builder()
 *       .command("/bin/bash-language-server", "start")
 *       .attachStorage()
 *       .withEnv(mapOf("HOME" to "/root"))
 *       .launch()
 *
 * Obtain via [PluginApi.process].
 */
interface IProcessApi {

    /**
     * Create a new process builder for launching a command inside the IDE environment.
     */
    fun builder(): ProcessLauncher

    interface ProcessLauncher {
        /** The executable and its arguments as they appear inside the rootfs. */
        fun command(vararg args: String): ProcessLauncher

        /**
         * Bind a host-filesystem directory into the proot environment.
         * [mountAt] is the rootfs-relative path where it will appear (e.g. "/root/lsp").
         * If omitted, the directory is mounted at the same absolute path.
         */
        fun attachDir(hostDir: java.io.File, mountAt: String? = null): ProcessLauncher

        /**
         * Bind the IDE's internal storage into the process environment.
         * Required for most language servers so they can reach the SDK, home dir, etc.
         */
        fun attachStorage(): ProcessLauncher

        /** Merge additional environment variables into the process environment. */
        fun withEnv(env: Map<String, String>): ProcessLauncher

        /** Launch the process and return a standard [Process] handle. */
        fun launch(): Process
    }
}