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

import androidx.compose.runtime.Composable

/**
 * Public API for rendering Compose UI overlays in the IDE.
 *
 * Plugins render arbitrary Compose UI into the IDE's live composition tree
 * through this interface. The IDE owns the actual ComposeView and Recomposer.
 * The plugin contributes @Composable lambdas that run inside the existing
 * EditorActivity composition.
 *
 * Obtain an instance via [PluginApi.ui].
 *
 * Usage:
 *   val handle = PluginApi.ui.showOverlay { handle ->
 *       MyPluginDialog(onDismiss = { handle.dismiss() })
 *   }
 *
 *   handle.dismiss()
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface IUiApi {

    /**
     * Show an overlay composable on top of the editor UI.
     *
     * The [content] lambda receives an [OverlayHandle] so it can dismiss itself
     * from within, e.g. from a button click.
     *
     * Must be called from the main thread. Use [PluginContext.runOnUiThread]
     * if calling from a background thread.
     *
     * The overlay appears immediately and stays until [OverlayHandle.dismiss] is called.
     *
     * @param content The composable content to display. Receives an [OverlayHandle]
     *                for dismissing the overlay.
     * @return An [OverlayHandle] to dismiss the overlay later.
     */
    fun showOverlay(content: @Composable (handle: OverlayHandle) -> Unit): OverlayHandle

    /**
     * Dismiss every overlay currently shown via this API instance.
     */
    fun dismissAll()
}

/**
 * A handle to a live overlay.
 *
 * Call [dismiss] to remove it from the UI.
 */
interface OverlayHandle {
    /**
     * Remove this overlay from the composition.
     *
     * Safe to call multiple times.
     */
    fun dismiss()

    /**
     * True while the overlay is still in the composition.
     */
    val isShowing: Boolean
}
