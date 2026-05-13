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

    /**
     * Install one or more packages via the distro package manager inside the
     * proot sandbox.
     *
     * The IDE resolves the correct invocation (apt-get, pacman, …) and runs it
     * through the sandbox entry-point that already holds the uid-mapping needed
     * to appear as root inside proot. Plugins must never construct apt-get or
     * sudo invocations themselves — the process sandbox rejects sudo, and direct
     * apt-get calls bypass the uid mapping required for dpkg to write its state.
     *
     * @param packages  One or more package names, e.g. `installPackage("clangd")`.
     * @return          A [Process] whose stdout/stderr stream the installation
     *                  output. Wait on [Process.waitFor]; exit code 0 = success.
     */
    fun installPackage(vararg packages: String): Process

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
         * Bind the Android SDK directory into the proot environment,
         * mounted at its standard rootfs path.
         */
        fun attachAndroidSdk(): ProcessLauncher
        
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