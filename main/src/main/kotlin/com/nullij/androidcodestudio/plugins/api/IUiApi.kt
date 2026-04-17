package com.nullij.androidcodestudio.plugins.api

import androidx.compose.runtime.Composable

/**
 * Public UI API for plugin use.
 *
 * Plugins render arbitrary Compose UI into the IDE's live composition tree
 * through this interface. The IDE owns the actual ComposeView and Recomposer |
 * the plugin just contributes @Composable lambdas that run inside the existing
 * EditorActivity composition.
 *
 * Obtain via [PluginApi.ui].
 *
 * Usage:
 *
 *   val handle = PluginApi.ui.showOverlay { handle ->
 *       MyPluginDialog(onDismiss = { handle.dismiss() })
 *   }
 *
 *   // Dismiss from outside:
 *   handle.dismiss()
 */
interface IUiApi {

    /**
     * Show an overlay composable on top of the editor UI.
     *
     * [content] is a @Composable lambda. It receives an [OverlayHandle] so it
     * can dismiss itself from within (e.g. from a button click).
     *
     * Must be called from the main thread, or use [PluginContext.runOnUiThread].
     * The overlay appears immediately and stays until [OverlayHandle.dismiss] is called.
     *
     * @return An [OverlayHandle] to dismiss the overlay later.
     */
    fun showOverlay(content: @Composable (handle: OverlayHandle) -> Unit): OverlayHandle

    /**
     * Dismiss every overlay currently shown via this API instance.
     */
    fun dismissAll()
}

/**
 * A handle to a live overlay. Call [dismiss] to remove it from the UI.
 */
interface OverlayHandle {
    /** Remove this overlay from the composition. Safe to call multiple times. */
    fun dismiss()
    /** True while the overlay is still in the composition. */
    val isShowing: Boolean
}