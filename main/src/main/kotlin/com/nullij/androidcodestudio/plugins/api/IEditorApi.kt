package com.nullij.androidcodestudio.plugins.api

/**
 * Public Editor API for plugin use.
 *
 * Plugins interact with the active file editor exclusively through this
 * interface.  No internal Editor, CodeEditor, or Sora-editor class is
 * reachable from plugin code.
 *
 * All methods are safe to call from any thread; implementations
 * marshal to the correct thread internally.
 *
 * Obtain an instance via [PluginApi.editor].
 */
interface IEditorApi {

    // ─── Availability ────────────────────────────────────────────────────────

    /**
     * Returns true if an editor is currently visible and ready.
     * Always check this before calling other methods | operations on an absent
     * editor are no-ops and return sensible defaults.
     */
    fun isAvailable(): Boolean

    // ─── Text content ────────────────────────────────────────────────────────

    /** Full text content of the currently open file. Empty string if no editor. */
    fun getText(): String

    /**
     * Replace the entire document content with [text].
     * The operation is recorded in the undo history.
     */
    fun setText(text: String)

    // ─── Cursor ──────────────────────────────────────────────────────────────

    /** 0-indexed line of the cursor. */
    fun getCurrentLine(): Int

    /** 0-indexed column of the cursor. */
    fun getCurrentColumn(): Int

    /** 1-indexed line | suitable for status bar display. */
    fun getCurrentLineDisplay(): Int

    /** 1-indexed column | suitable for status bar display. */
    fun getCurrentColumnDisplay(): Int

    /**
     * Move the cursor to the given [line] / [column] (both 0-indexed).
     * Clamps to valid bounds silently.
     */
    fun setCursor(line: Int, column: Int)

    // ─── Selection ───────────────────────────────────────────────────────────

    /** True if a text range is currently selected. */
    fun hasSelection(): Boolean

    /**
     * Returns the currently selected text, or null if nothing is selected.
     */
    fun getSelectedText(): String?

    /** Select all text in the document. */
    fun selectAll()

    // ─── Clipboard ───────────────────────────────────────────────────────────

    /** Copy selected text to the clipboard. No-op if nothing is selected. */
    fun copy()

    /** Cut selected text to the clipboard. No-op if nothing is selected. */
    fun cut()

    /** Paste clipboard content at the current cursor position. */
    fun paste()

    // ─── Undo / Redo ─────────────────────────────────────────────────────────

    fun canUndo(): Boolean
    fun canRedo(): Boolean

    fun undo()
    fun redo()

    // ─── Insert / Delete ─────────────────────────────────────────────────────

    /**
     * Insert [text] at the current cursor position.
     * This is the preferred way for plugins to inject code snippets.
     */
    fun insertText(text: String)

    /**
     * Delete the currently selected range.
     * No-op if nothing is selected.
     */
    fun deleteSelection()

    // ─── Formatting ──────────────────────────────────────────────────────────

    /**
     * Trigger an async format of the entire document (delegates to the active
     * language server formatter if available).
     */
    fun formatDocument()
}
