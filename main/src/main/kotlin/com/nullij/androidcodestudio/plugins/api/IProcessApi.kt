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

/**
 * Public API for launching managed processes inside the IDE's proot/acsenv environment.
 *
 * Plugin language servers use this to start their server executable. The IDE
 * handles environment setup, storage binding, and process lifecycle. The plugin
 * provides only the command to run and any required environment configuration.
 *
 * Usage:
 *   val process = PluginApi.process
 *       .builder()
 *       .command("/bin/bash-language-server", "start")
 *       .attachStorage()
 *       .withEnv(mapOf("HOME" to "/root"))
 *       .launch()
 *
 * Obtain an instance via [PluginApi.process].
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface IProcessApi {

    /**
     * Create a new process builder for launching a command inside the IDE environment.
     *
     * @return A [ProcessLauncher] instance for configuring the process.
     */
    fun builder(): ProcessLauncher

    /**
     * Install one or more packages via the distro package manager inside the proot sandbox.
     *
     * The IDE resolves the correct invocation (apt-get, pacman, etc.) and runs it
     * through the sandbox entry-point that holds the uid-mapping required to appear
     * as root inside proot. Plugins must never construct apt-get or sudo invocations
     * themselves. The process sandbox rejects sudo, and direct apt-get calls bypass
     * the uid mapping required for dpkg to write its state.
     *
     * @param packages One or more package names, e.g. `installPackage("clangd")`.
     * @return A [Process] whose stdout/stderr streams contain the installation output.
     *         Call [Process.waitFor] to wait for completion. Exit code 0 indicates success.
     */
    fun installPackage(vararg packages: String): Process

    interface ProcessLauncher {
        /**
         * Set the executable and its arguments as they appear inside the rootfs.
         *
         * @param args The command and its arguments.
         * @return This builder instance for chaining.
         */
        fun command(vararg args: String): ProcessLauncher

        /**
         * Bind a host-filesystem directory into the proot environment.
         *
         * @param hostDir The directory on the host filesystem to bind.
         * @param mountAt The rootfs-relative path where the directory will appear.
         *                If omitted, the directory mounts at the same absolute path.
         * @return This builder instance for chaining.
         */
        fun attachDir(hostDir: java.io.File, mountAt: String? = null): ProcessLauncher

        /**
         * Bind the Android SDK directory into the proot environment, mounted at its
         * standard rootfs path.
         *
         * @return This builder instance for chaining.
         */
        fun attachAndroidSdk(): ProcessLauncher

        /**
         * Bind the IDE's internal storage into the process environment.
         *
         * Required for most language servers so they can reach the SDK, home directory,
         * and other storage locations.
         *
         * @return This builder instance for chaining.
         */
        fun attachStorage(): ProcessLauncher

        /**
         * Merge additional environment variables into the process environment.
         *
         * @param env Map of environment variable names to values.
         * @return This builder instance for chaining.
         */
        fun withEnv(env: Map<String, String>): ProcessLauncher

        /**
         * Launch the process and return a standard [Process] handle.
         *
         * @return The running [Process] instance.
         */
        fun launch(): Process
    }
}