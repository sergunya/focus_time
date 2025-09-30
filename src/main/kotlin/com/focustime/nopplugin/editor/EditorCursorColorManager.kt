package com.focustime.nopplugin.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Deprecated: Color-based caret replacement removed. This service is now a no-op and kept only for backward compatibility.
 */
@Service(Service.Level.PROJECT)
class EditorCursorColorManager(@Suppress("UNUSED_PARAMETER") private val project: Project) {
    fun setEnabled(@Suppress("UNUSED_PARAMETER") enabled: Boolean) { /* no-op */ }
    fun updateColor(@Suppress("UNUSED_PARAMETER") color: java.awt.Color) { /* no-op */ }
    fun initializeFromSettings(@Suppress("UNUSED_PARAMETER") settings: Any) { /* no-op */ }
}
