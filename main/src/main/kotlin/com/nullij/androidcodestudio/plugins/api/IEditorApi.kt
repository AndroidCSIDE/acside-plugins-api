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
 * Public Editor API for plugin use.
 *
 * Plugins interact with the active file editor exclusively through this
 * interface. No internal Editor, CodeEditor, or Sora-editor class is
 * reachable from plugin code.
 *
 * All methods are safe to call from any thread; implementations marshal
 * to the correct thread internally.
 *
 * Obtain an instance via [PluginApi.editor].
 *
 * @author nullij
 * @see https://github.com/nullij
 */
interface IEditorApi {

    /**
     * Returns true if an editor is currently visible and ready.
     * Always check this before calling other methods — operations on an absent
     * editor are no-ops and return sensible defaults.
     */
    fun isAvailable(): Boolean

    /**
     * Full text content of the currently open file. Empty string if no editor.
     */
    fun getText(): String

    /**
     * Replace the entire document content with [text].
     * The operation is recorded in the undo history.
     */
    fun setText(text: String)

    /**
     * 0-indexed line of the cursor.
     */
    fun getCurrentLine(): Int

    /**
     * 0-indexed column of the cursor.
     */
    fun getCurrentColumn(): Int

    /**
     * 1-indexed line, suitable for status bar display.
     */
    fun getCurrentLineDisplay(): Int

    /**
     * 1-indexed column, suitable for status bar display.
     */
    fun getCurrentColumnDisplay(): Int

    /**
     * Move the cursor to the given [line] / [column] (both 0-indexed).
     * Clamps to valid bounds silently.
     */
    fun setCursor(line: Int, column: Int)

    /**
     * True if a text range is currently selected.
     */
    fun hasSelection(): Boolean

    /**
     * Returns the currently selected text, or null if nothing is selected.
     */
    fun getSelectedText(): String?

    /**
     * Select all text in the document.
     */
    fun selectAll()

    /**
     * Copy selected text to the clipboard. No-op if nothing is selected.
     */
    fun copy()

    /**
     * Cut selected text to the clipboard. No-op if nothing is selected.
     */
    fun cut()

    /**
     * Paste clipboard content at the current cursor position.
     */
    fun paste()

    /**
     * Returns true if there is an undo action available.
     */
    fun canUndo(): Boolean

    /**
     * Returns true if there is a redo action available.
     */
    fun canRedo(): Boolean

    /**
     * Undo the last edit operation.
     */
    fun undo()

    /**
     * Redo the last undone edit operation.
     */
    fun redo()

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

    /**
     * Trigger an async format of the entire document. Delegates to the active
     * language server formatter if available.
     */
    fun formatDocument()
}
