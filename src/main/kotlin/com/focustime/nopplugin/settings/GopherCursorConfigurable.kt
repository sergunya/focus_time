package com.focustime.nopplugin.settings

import com.focustime.nopplugin.editor.EditorGopherCursorInstaller
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class GopherCursorConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var checkBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Focus Time: Gopher Editor Cursor"

    override fun createComponent(): JComponent {
        val settings = GopherCursorSettings.getInstance()

        val cb = JBCheckBox("Replace the editor caret with a Gopher icon")
        cb.isSelected = settings.settingsState.enabled
        checkBox = cb

        val p = FormBuilder.createFormBuilder()
            .addComponent(cb)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel = p
        return p
    }

    override fun isModified(): Boolean {
        val settings = GopherCursorSettings.getInstance()
        return checkBox?.isSelected != settings.settingsState.enabled
    }

    override fun apply() {
        val settings = GopherCursorSettings.getInstance()
        val newEnabled = checkBox?.isSelected ?: true
        val enabledChanged = settings.settingsState.enabled != newEnabled

        settings.settingsState.enabled = newEnabled

        if (enabledChanged) {
            project.getService(EditorGopherCursorInstaller::class.java)?.setEnabled(newEnabled)
        }
    }

    override fun reset() {
        val settings = GopherCursorSettings.getInstance()
        checkBox?.isSelected = settings.settingsState.enabled
    }

    override fun disposeUIResources() {
        panel = null
        checkBox = null
    }
}

