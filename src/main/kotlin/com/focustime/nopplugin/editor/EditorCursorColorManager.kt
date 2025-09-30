package com.focustime.nopplugin.editor

import com.focustime.nopplugin.settings.GopherCursorSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import java.awt.Color

/**
 * Applies the configured caret (cursor) color to all IDE editors.
 * Keeps track of the original caret color to restore it when disabled.
 */
@Service(Service.Level.PROJECT)
class EditorCursorColorManager(private val project: Project) {
    private var enabled: Boolean = true
    private var currentColor: Color = Color.WHITE
    private var originalColor: Color? = null

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        applyCurrent()
    }

    fun updateColor(color: Color) {
        this.currentColor = color
        if (enabled) applyCurrent()
    }

    private fun applyCurrent() {
        val colorsManager = EditorColorsManager.getInstance()
        val scheme = colorsManager.globalScheme

        if (enabled) {
            if (originalColor == null) {
                originalColor = scheme.getColor(EditorColors.CARET_COLOR)
            }
            scheme.setColor(EditorColors.CARET_COLOR, currentColor)
        } else {
            // restore original color (may be null => scheme default)
            scheme.setColor(EditorColors.CARET_COLOR, originalColor)
            // Only clear remembered original when disabled explicitly to avoid losing it across color updates
        }

        // Refresh editors to apply color instantly
        EditorFactory.getInstance().refreshAllEditors()
    }

    fun initializeFromSettings(settings: GopherCursorSettings.State) {
        this.currentColor = Color(settings.cursorColor, true)
        setEnabled(settings.enabled)
    }
}
